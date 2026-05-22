(function () {
    const panel = document.querySelector("[data-route-suggestions-url]");

    if (!panel) {
        return;
    }

    const list = panel.querySelector("[data-route-suggestions-list]");
    const status = panel.querySelector("[data-route-suggestions-status]");
    const routeSummary = panel.querySelector("[data-route-summary]");
    const mapElement = document.getElementById("travelgroup-route-map");
    const recenterButton = panel.querySelector("[data-route-map-recenter]");
    const routeForm = panel.querySelector("[data-route-form]");
    const originInput = document.getElementById("route-origin");
    const destinationInput = document.getElementById("route-destination");
    const originSuggestions = document.getElementById("route-origin-suggestions");
    const destinationSuggestions = document.getElementById("route-destination-suggestions");
    const swapButton = panel.querySelector("[data-route-swap]");
    const resetButton = panel.querySelector("[data-route-reset]");
    const departureInput = document.getElementById("route-departure-time");
    const useNowButton = panel.querySelector("[data-route-use-now]");
    let routeMap = null;
    let routeBounds = null;
    let selectedRouteLine = null;
    let originState = {
        label: panel.dataset.defaultOriginLabel,
        latitude: Number(panel.dataset.defaultOriginLatitude),
        longitude: Number(panel.dataset.defaultOriginLongitude)
    };
    let destinationState = {
        label: panel.dataset.defaultDestinationLabel,
        latitude: Number(panel.dataset.defaultDestinationLatitude),
        longitude: Number(panel.dataset.defaultDestinationLongitude)
    };
    let departureState = panel.dataset.defaultDepartureTime || "";
    let didUseNearestStopsAsInitialFields = false;
    let originStopSuggestions = [];
    let destinationStopSuggestions = [];
    let originSuggestTimeout = null;
    let destinationSuggestTimeout = null;

    // Escapes user/API text before inserting it into rendered route cards
    const escapeHtml = (value) => String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");

    // Formats De Lijn date-times for compact display in the route cards
    const formatDateTime = (value) => {
        if (!value) {
            return "Time pending";
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value.replace("T", " ");
        }

        return new Intl.DateTimeFormat(undefined, {
            day: "2-digit",
            month: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        }).format(date);
    };

    // Builds a readable stop label from the stop name and municipality
    const stopLabel = (stop, fallback) => {
        if (!stop) {
            return fallback;
        }

        const parts = [stop.name || stop.description, stop.municipality].filter(Boolean);
        return parts.length > 0 ? parts.join(", ") : fallback;
    };

    // Checks whether a pair of coordinates can be used by the map/API
    const hasCoordinates = (lat, lng) => Number.isFinite(Number(lat)) && Number.isFinite(Number(lng));

    // Escapes marker labels before passing them to Leaflet popups
    const markerLabel = (label, fallback) => escapeHtml(label || fallback);

    // Formats a stop label for an option, preserving the provided fallback
    const optionStopLabel = (stop, fallback) => stopLabel(stop, fallback);

    // Synchronizes form field values with the route state object
    const setInputsFromState = () => {
        if (originInput) {
            originInput.value = originState.label || "";
        }
        if (destinationInput) {
            destinationInput.value = destinationState.label || "";
        }
        if (departureInput) {
            departureInput.value = departureState || "";
        }
    };

    // Converts a Date to the value format expected by datetime-local inputs
    const toDateTimeLocal = (date) => {
        const pad = (value) => String(value).padStart(2, "0");
        return [
            date.getFullYear(),
            pad(date.getMonth() + 1),
            pad(date.getDate())
        ].join("-") + "T" + [
            pad(date.getHours()),
            pad(date.getMinutes())
        ].join(":");
    };

    // Stores the currently selected departure time from the input
    const syncDepartureState = () => {
        if (departureInput) {
            departureState = departureInput.value || "";
        }
    };

    // Replaces the initial address labels with nearest De Lijn stops after the first API result loads
    const setInitialNearestStopFields = (payload) => {
        if (didUseNearestStopsAsInitialFields || !Array.isArray(payload.options) || payload.options.length === 0) {
            return;
        }

        const firstOption = payload.options.find((option) => option.originStop || option.destinationStop);
        if (!firstOption) {
            return;
        }

        if (firstOption.originStop && hasCoordinates(firstOption.originStop.latitude, firstOption.originStop.longitude)) {
            originState = {
                label: optionStopLabel(firstOption.originStop, payload.originLabel),
                latitude: Number(firstOption.originStop.latitude),
                longitude: Number(firstOption.originStop.longitude)
            };
        }

        if (firstOption.destinationStop && hasCoordinates(firstOption.destinationStop.latitude, firstOption.destinationStop.longitude)) {
            destinationState = {
                label: optionStopLabel(firstOption.destinationStop, payload.destinationLabel),
                latitude: Number(firstOption.destinationStop.latitude),
                longitude: Number(firstOption.destinationStop.longitude)
            };
        }

        setInputsFromState();
        didUseNearestStopsAsInitialFields = true;
    };

    // Converts a route payload into two Leaflet coordinate points
    const buildRoutePoints = (payload) => [
        [Number(payload.originLatitude), Number(payload.originLongitude)],
        [Number(payload.destinationLatitude), Number(payload.destinationLongitude)]
    ];

    // Gives Leaflet a few chances to recalculate after CSS/layout changes settle
    const settleRouteMap = () => {
        if (!routeMap) {
            return;
        }

        [80, 250, 700].forEach((delay) => {
            setTimeout(() => {
                routeMap.invalidateSize();
                fitRoute();
            }, delay);
        });
    };

    // Fits the map viewport to the current route bounds
    const fitRoute = () => {
        if (routeMap && routeBounds) {
            routeMap.fitBounds(routeBounds, {
                padding: [42, 42],
                maxZoom: 16
            });
        }
    };

    // Highlights a selected card and draws that route on the map
    const selectRouteOption = (payload, selectedCard) => {
        if (!routeMap || !window.L) {
            return;
        }

        panel.querySelectorAll(".travelgroup-route-card").forEach((card) => {
            card.classList.toggle("is-selected", card === selectedCard);
        });

        if (selectedRouteLine) {
            routeMap.removeLayer(selectedRouteLine);
        }

        selectedRouteLine = L.polyline(buildRoutePoints(payload), {
            color: "#1f5fa8",
            weight: 6,
            opacity: 0.9
        }).addTo(routeMap);
        routeBounds = selectedRouteLine.getBounds();
        fitRoute();
        settleRouteMap();
    };

    // Removes all map overlays while keeping the OpenStreetMap tile layer
    const clearRouteMap = () => {
        if (!routeMap) {
            return;
        }

        routeMap.eachLayer((layer) => {
            if (!(layer instanceof L.TileLayer)) {
                routeMap.removeLayer(layer);
            }
        });
        selectedRouteLine = null;
    };

    // Initializes or redraws the map for the current origin/destination payload
    const drawRouteMap = (payload) => {
        if (!mapElement || !window.L) {
            return;
        }

        const origin = [Number(payload.originLatitude), Number(payload.originLongitude)];
        const destination = [Number(payload.destinationLatitude), Number(payload.destinationLongitude)];

        if (!hasCoordinates(origin[0], origin[1]) || !hasCoordinates(destination[0], destination[1])) {
            mapElement.classList.add("travelgroup-route-suggestions__map--hidden");
            return;
        }

        if (!routeMap) {
            // Leaflet is created lazily so hidden/unsupported pages do not initialize an unused map
            routeMap = L.map(mapElement, {
                scrollWheelZoom: false
            });

            L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
                attribution: "&copy; OpenStreetMap contributors"
            }).addTo(routeMap);
        }

        clearRouteMap();

        L.marker(origin)
            .addTo(routeMap)
            .bindPopup(markerLabel(payload.originLabel, "Departure point"));
        L.marker(destination)
            .addTo(routeMap)
            .bindPopup(markerLabel(payload.destinationLabel, "Activity destination"));

        selectedRouteLine = L.polyline([origin, destination], {
            color: "#2f7fa3",
            weight: 5,
            opacity: 0.85,
            dashArray: "8 8"
        }).addTo(routeMap);

        routeBounds = L.latLngBounds([origin, destination]);
        fitRoute();
        settleRouteMap();
    };

    // Builds the backend route suggestions URL from the current form/map state
    const buildSuggestionsUrl = () => {
        const url = new URL(panel.dataset.routeSuggestionsUrl, window.location.origin);
        url.searchParams.set("originLatitude", originState.latitude);
        url.searchParams.set("originLongitude", originState.longitude);
        url.searchParams.set("originLabel", originState.label || "");
        url.searchParams.set("destinationLatitude", destinationState.latitude);
        url.searchParams.set("destinationLongitude", destinationState.longitude);
        url.searchParams.set("destinationLabel", destinationState.label || "");
        if (departureState) {
            url.searchParams.set("departureTime", departureState);
        }
        return url.toString();
    };

    // Uses Nominatim as a last-resort geocoder when no De Lijn stop suggestion matches user text
    const geocodeLocation = async (query) => {
        const response = await fetch(`https://nominatim.openstreetmap.org/search?format=json&countrycodes=be&limit=1&q=${encodeURIComponent(query)}`);
        if (!response.ok) {
            throw new Error("Location search unavailable");
        }

        const results = await response.json();
        if (!Array.isArray(results) || results.length === 0) {
            throw new Error("Location not found");
        }

        return {
            label: query,
            latitude: Number(results[0].lat),
            longitude: Number(results[0].lon)
        };
    };

    // Creates a Google Maps transit URL for opening an option outside the app
    const googleMapsTransitUrl = (option) => {
        const originStop = option.originStop || {};
        const destinationStop = option.destinationStop || {};
        const originLat = Number(originStop.latitude || originState.latitude);
        const originLng = Number(originStop.longitude || originState.longitude);
        const destinationLat = Number(destinationStop.latitude || destinationState.latitude);
        const destinationLng = Number(destinationStop.longitude || destinationState.longitude);
        const url = new URL("https://www.google.com/maps/dir/");

        if (hasCoordinates(originLat, originLng) && hasCoordinates(destinationLat, destinationLng)) {
            url.pathname = `/maps/dir/${originLat},${originLng}/${destinationLat},${destinationLng}/`;
        }

        url.searchParams.set("travelmode", "transit");
        return url.toString();
    };

    // Builds the datalist label for a suggested stop
    const stopSuggestionLabel = (stop) => stopLabel(stop, "De Lijn stop");

    // Renders stop suggestions into a datalist element
    const renderStopSuggestions = (datalist, stops) => {
        if (!datalist) {
            return;
        }

        datalist.innerHTML = stops.map((stop) => `
            <option value="${escapeHtml(stopSuggestionLabel(stop))}"></option>
        `).join("");
    };

    // Loads stop suggestions from the backend, optionally biased by the current coordinate
    const loadStopSuggestionResults = async (query, maxResults = 15, referenceState = originState) => {
        const url = new URL("/api/delijn/stop-suggestions", window.location.origin);
        url.searchParams.set("q", query);
        url.searchParams.set("maxResults", String(maxResults));
        if (hasCoordinates(referenceState.latitude, referenceState.longitude)) {
            url.searchParams.set("lat", referenceState.latitude);
            url.searchParams.set("lng", referenceState.longitude);
        }

        const response = await fetch(url);
        if (!response.ok) {
            return [];
        }

        const stops = await response.json();
        return Array.isArray(stops) ? stops : [];
    };

    // Fetches stop suggestions and stores them in the matching state list
    const fetchStopSuggestions = async (query, datalist, assignStops, referenceState = originState) => {
        if (!query || query.length < 2) {
            assignStops([]);
            renderStopSuggestions(datalist, []);
            return;
        }

        const stops = await loadStopSuggestionResults(query, 15, referenceState);
        assignStops(stops);
        renderStopSuggestions(datalist, stops);
    };

    // Prefers rail-like stops for Antwerp city-center searches when several suggestions are returned
    const isTramishStop = (stop) => {
        const text = `${stop?.name || ""} ${stop?.municipality || ""}`.toLowerCase();
        return text.includes("metro")
            || text.includes("premetro")
            || text.includes("tram")
            || text.includes("groenplaats")
            || text.includes("elisabeth")
            || text.includes("astrid");
    };

    // Resolves an input value to coordinates using selected suggestions, De Lijn search, then geocoding
    const stateFromInput = async (input, suggestions, referenceState = originState) => {
        const query = input.value.trim();
        const matchedStop = suggestions.find((stop) => stopSuggestionLabel(stop) === query);
        if (matchedStop && hasCoordinates(matchedStop.latitude, matchedStop.longitude)) {
            return {
                label: stopSuggestionLabel(matchedStop),
                latitude: Number(matchedStop.latitude),
                longitude: Number(matchedStop.longitude)
            };
        }

        const suggestionStops = await loadStopSuggestionResults(query, 15, referenceState);
        // Choose a tram/metro-like stop first because the KdG use case often targets Antwerp public transport hubs
        const bestStop = suggestionStops.find((stop) => isTramishStop(stop) && hasCoordinates(stop.latitude, stop.longitude))
            || suggestionStops.find((stop) => hasCoordinates(stop.latitude, stop.longitude));
        if (bestStop) {
            return {
                label: stopSuggestionLabel(bestStop),
                latitude: Number(bestStop.latitude),
                longitude: Number(bestStop.longitude)
            };
        }

        return geocodeLocation(query);
    };

    // Renders an empty/error state into the transport options strip
    const renderEmpty = (message, icon) => {
        list.innerHTML = `
            <div class="travelgroup-route-suggestions__empty">
                <div>
                    <i class="bi ${icon}" aria-hidden="true"></i>
                    <strong>${escapeHtml(message)}</strong>
                </div>
                <span>Showing the route markers below. Try another date/time or location if De Lijn does not return departures for this selection.</span>
            </div>
        `;
    };

    // Updates the summary text shown under the route form
    const renderRouteSummary = (payload, options = []) => {
        if (!routeSummary) {
            return;
        }

        const summary = transportSummary(options);
        const coverage = payload.transitCoverage || "De Lijn covers buses and trams.";
        const notice = options.some((option) => !option.realtime)
            ? "These are scheduled bus/tram departures for the selected date and time."
            : "These are live departures available now near the group start.";

        routeSummary.innerHTML = `
            <strong>${escapeHtml(coverage)}</strong>
            <span>${escapeHtml(notice)}</span>
            ${summary ? `<span>${escapeHtml(summary)}</span>` : ""}
        `;
    };

    // Renders one transport option card
    const renderOption = (option, index) => {
        const lineNumbers = Array.isArray(option.lineNumbers) && option.lineNumbers.length > 0
            ? option.lineNumbers.map(escapeHtml).join(", ")
            : "Line pending";
        const origin = stopLabel(option.originStop, "Nearby departure stop");
        const destination = option.lineDestination
            ? `Direction ${option.lineDestination}`
            : stopLabel(option.destinationStop, "Nearby arrival stop");
        const scheduleLabel = option.realtime ? "Live now" : "Scheduled";
        const transportType = routeTransportType(option.transportType);
        const plannerUrl = googleMapsTransitUrl(option);

        return `
            <article class="travelgroup-route-card" data-route-option-index="${index}">
                <div class="travelgroup-route-card__mode" title="De Lijn ${escapeHtml(transportType.label.toLowerCase())}">
                    <i class="bi ${transportType.icon}" aria-hidden="true"></i>
                    <span>${lineNumbers}</span>
                </div>
                <div class="travelgroup-route-card__times">
                    <a class="travelgroup-route-card__external"
                       href="${escapeHtml(plannerUrl)}"
                       target="_blank"
                       rel="noopener noreferrer"
                       title="Open this route in Google Maps">
                        <i class="bi bi-box-arrow-up-right" aria-hidden="true"></i>
                        <span class="visually-hidden">Open in Google Maps</span>
                    </a>
                    <span>
                        ${escapeHtml(formatDateTime(option.departureTime))}
                    </span>
                    <span>
                        <i class="bi ${option.realtime ? "bi-broadcast" : "bi-calendar-check"}" aria-hidden="true"></i>
                        ${scheduleLabel}
                    </span>
                </div>
                <div class="travelgroup-route-card__body">
                    <strong>${escapeHtml(origin)}</strong>
                    <span>${escapeHtml(destination)}</span>
                    <button type="button" class="travelgroup-route-card__details">Show on map</button>
                </div>
                <div class="travelgroup-route-card__meta">
                    <span class="travelgroup-route-card__line">
                        <i class="bi ${transportType.icon}" aria-hidden="true"></i>
                        ${escapeHtml(transportType.label)}
                    </span>
                    <span class="travelgroup-route-card__line travelgroup-route-card__line--brand">
                        De Lijn
                    </span>
                    <span class="travelgroup-route-card__pill">${option.realtime ? "Live" : "Scheduled"}</span>
                </div>
            </article>
        `;
    };

    // Maps backend transport types to labels and Bootstrap icons
    const routeTransportType = (transportType) => {
        const normalized = String(transportType || "").toUpperCase();
        if (normalized === "TRAM") {
            return {label: "Tram", icon: "bi-train-front"};
        }

        if (normalized === "BUS") {
            return {label: "Bus", icon: "bi-bus-front"};
        }

        return {label: "Bus/tram", icon: "bi-bus-front"};
    };

    // Counts available options by transport type for the summary card
    const transportSummary = (options) => {
        const counts = options.reduce((result, option) => {
            const type = routeTransportType(option.transportType).label;
            result[type] = (result[type] || 0) + 1;
            return result;
        }, {});

        return ["Bus", "Tram", "Bus/tram"]
            .filter((type) => counts[type])
            .map((type) => `${type}: ${counts[type]}`)
            .join(" · ");
    };

    // Loads suggestions, updates the summary/cards, and refreshes the map
    const loadSuggestions = (url = buildSuggestionsUrl(), useNearestStopsForFields = false) => fetch(url)
        .then((response) => {
            if (!response.ok) {
                throw new Error("Route suggestions unavailable");
            }
            return response.json();
        })
        .then((payload) => {
            status.textContent = payload.configured ? "De Lijn API" : "Not configured";
            if (useNearestStopsForFields) {
                setInitialNearestStopFields(payload);
                // Keep the map payload aligned with the nearest-stop labels we just promoted into the form fields.
                payload = {
                    ...payload,
                    originLabel: originState.label,
                    originLatitude: originState.latitude,
                    originLongitude: originState.longitude,
                    destinationLabel: destinationState.label,
                    destinationLatitude: destinationState.latitude,
                    destinationLongitude: destinationState.longitude
                };
            }
            drawRouteMap(payload);

            if (!payload.supported || !Array.isArray(payload.options) || payload.options.length === 0) {
                renderRouteSummary(payload, []);
                renderEmpty(payload.message || "No route suggestions available.", "bi-signpost-split");
                return;
            }

            const hasScheduled = payload.options.some((option) => !option.realtime);
            status.textContent = hasScheduled
                ? `${payload.options.length} scheduled departure${payload.options.length === 1 ? "" : "s"}`
                : `${payload.options.length} live departure${payload.options.length === 1 ? "" : "s"} now`;
            renderRouteSummary(payload, payload.options);
            list.innerHTML = `
                ${payload.options.map(renderOption).join("")}
            `;

            panel.querySelectorAll("[data-route-option-index]").forEach((card) => {
                const externalLink = card.querySelector(".travelgroup-route-card__external");
                if (externalLink) {
                    externalLink.addEventListener("click", (event) => {
                        event.stopPropagation();
                    });
                }

                card.addEventListener("click", () => {
                    selectRouteOption(payload, card);
                });
            });
            settleRouteMap();
        })
        .catch(() => {
            status.textContent = "Unavailable";
            if (routeSummary) {
                routeSummary.innerHTML = `
                    <strong>De Lijn route suggestions are unavailable.</strong>
                    <span>Please try again later or check another route.</span>
                `;
            }
            renderEmpty("De Lijn route suggestions are temporarily unavailable.", "bi-wifi-off");
        });

    setInputsFromState();
    loadSuggestions(buildSuggestionsUrl(), true);

    if (recenterButton) {
        // Refit the current route without changing the selected card.
        recenterButton.addEventListener("click", fitRoute);
    }

    if (routeForm) {
        // Recalculate route options after the user submits edited origin/destination text
        routeForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            status.textContent = "Searching route...";

            try {
                syncDepartureState();
                const [origin, destination] = await Promise.all([
                    stateFromInput(originInput, originStopSuggestions, originState),
                    stateFromInput(destinationInput, destinationStopSuggestions, destinationState)
                ]);
                originState = origin;
                destinationState = destination;
                didUseNearestStopsAsInitialFields = true;
                await loadSuggestions(buildSuggestionsUrl(), false);
            } catch (error) {
                status.textContent = "Location not found";
                renderEmpty("Could not find one of those locations.", "bi-search");
            }
        });
    }

    if (swapButton) {
        // Swap origin and destination, then reload suggestions for the reversed route
        swapButton.addEventListener("click", () => {
            const previousOrigin = originState;
            originState = destinationState;
            destinationState = previousOrigin;
            didUseNearestStopsAsInitialFields = true;
            setInputsFromState();
            loadSuggestions(buildSuggestionsUrl(), false);
        });
    }

    if (resetButton) {
        // Restore the group defaults from the Thymeleaf data attributes
        resetButton.addEventListener("click", () => {
            originState = {
                label: panel.dataset.defaultOriginLabel,
                latitude: Number(panel.dataset.defaultOriginLatitude),
                longitude: Number(panel.dataset.defaultOriginLongitude)
            };
            destinationState = {
                label: panel.dataset.defaultDestinationLabel,
                latitude: Number(panel.dataset.defaultDestinationLatitude),
                longitude: Number(panel.dataset.defaultDestinationLongitude)
            };
            didUseNearestStopsAsInitialFields = false;
            departureState = panel.dataset.defaultDepartureTime || "";
            setInputsFromState();
            loadSuggestions(buildSuggestionsUrl(), false);
        });
    }

    if (useNowButton) {
        // Replace the departure time with the current local time and refresh the route options
        useNowButton.addEventListener("click", () => {
            departureState = toDateTimeLocal(new Date());
            setInputsFromState();
            loadSuggestions(buildSuggestionsUrl(), false);
        });
    }

    if (departureInput) {
        // Store manual departure-time changes for the next search.
        departureInput.addEventListener("change", () => {
            syncDepartureState();
        });
    }

    if (originInput) {
        // Debounce origin suggestions so typing does not call the backend on every keystroke
        originInput.addEventListener("input", () => {
            clearTimeout(originSuggestTimeout);
            originSuggestTimeout = setTimeout(() => {
                fetchStopSuggestions(
                    originInput.value.trim(),
                    originSuggestions,
                    (stops) => {
                        originStopSuggestions = stops;
                    },
                    originState
                ).catch(() => {});
            }, 250);
        });
    }

    if (destinationInput) {
        // Debounce destination suggestions separately from origin suggestions
        destinationInput.addEventListener("input", () => {
            clearTimeout(destinationSuggestTimeout);
            destinationSuggestTimeout = setTimeout(() => {
                fetchStopSuggestions(
                    destinationInput.value.trim(),
                    destinationSuggestions,
                    (stops) => {
                        destinationStopSuggestions = stops;
                    },
                    destinationState
                ).catch(() => {});
            }, 250);
        });
    }
})();