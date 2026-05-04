class TravelGroupMap {
    constructor(options) {
        this.mapElement = options.mapElement;
        this.locationInput = options.locationInput || null;
        this.activitySelect = options.activitySelect || null;
        this.activityLocationButton = options.activityLocationButton || null;
        this.transportModeSelect = options.transportModeSelect || null;
        this.transportModeIcon = options.transportModeIcon || null;
        this.transportModeButtons = options.transportModeButtons || [];
        this.mapsLink = options.mapsLink || null;
        this.readOnly = options.readOnly || false;
        this.defaultCenter = [51.2194, 4.4025];
        this.marker = null;
        this.searchTimeout = null;
    }

    init() {
        if (!window.L || !this.mapElement) {
            return;
        }

        // Start around Antwerp so the map is useful even before a location is selected
        this.map = L.map(this.mapElement, {
            scrollWheelZoom: false
        }).setView(this.defaultCenter, 12);

        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            attribution: "&copy; OpenStreetMap contributors"
        }).addTo(this.map);

        this.bindCreateForm();
        this.bindTransportModeIcon();
        this.bindMapsLink();
        this.initReadOnlyMap();

        setTimeout(() => this.map.invalidateSize(), 100);
    }

    bindCreateForm() {
        if (this.readOnly || !this.locationInput) {
            return;
        }

        // When the user clicks the map, use that point as the meeting location
        this.map.on("click", async (event) => {
            const lat = event.latlng.lat;
            const lng = event.latlng.lng;
            const fallback = this.formatCoordinates(lat, lng);

            this.setMarker(lat, lng, fallback, true);

            try {
                const label = await this.reverseGeocode(lat, lng);
                this.setMarker(lat, lng, label, true);
            } catch (error) {
                this.locationInput.value = fallback;
            }
        });

        // Small debounce so we do not call the geocoder on every single key press.
        this.locationInput.addEventListener("input", () => {
            clearTimeout(this.searchTimeout);

            const query = this.locationInput.value.trim();
            if (query.length < 3) {
                return;
            }

            this.searchTimeout = setTimeout(() => {
                this.searchLocation(query).catch(() => {
                    // Keep manual entry usable when geocoding is unavailable
                });
            }, 450);
        });

        if (this.activitySelect) {
            this.activitySelect.addEventListener("change", () => {
                this.focusSelectedActivity(false);
            });
            this.focusSelectedActivity(false);
        }

        if (this.activityLocationButton) {
            this.activityLocationButton.addEventListener("click", () => {
                this.focusSelectedActivity(true);
            });
        }
    }

    bindTransportModeIcon() {
        if (!this.transportModeSelect || !this.transportModeIcon) {
            return;
        }

        // Keep the select, leading icon, and quick-choice buttons showing the same mode
        const updateIcon = () => {
            const selected = this.transportModeSelect.options[this.transportModeSelect.selectedIndex];
            const iconClass = selected?.dataset.icon || "bi-car-front";

            this.transportModeIcon.className = `bi ${iconClass}`;
            this.syncTransportModeButtons();
        };

        this.transportModeSelect.addEventListener("change", updateIcon);
        this.transportModeButtons.forEach((button) => {
            button.addEventListener("click", () => {
                this.transportModeSelect.value = button.dataset.mode;
                this.transportModeSelect.dispatchEvent(new Event("change", {bubbles: true}));
            });
        });
        updateIcon();
    }

    syncTransportModeButtons() {
        this.transportModeButtons.forEach((button) => {
            button.classList.toggle(
                "is-selected",
                button.dataset.mode === this.transportModeSelect.value
            );
        });
    }

    bindMapsLink() {
        if (!this.mapsLink) {
            return;
        }

        const query = this.mapsLink.dataset.mapsQuery;
        if (!query) {
            return;
        }

        this.mapsLink.href = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
    }

    initReadOnlyMap() {
        if (!this.readOnly) {
            return;
        }

        const lat = Number(this.mapElement.dataset.latitude);
        const lng = Number(this.mapElement.dataset.longitude);
        const location = this.mapElement.dataset.location || "Meeting point";

        if (location) {
            // Detail page should show the group's meeting point first, not only the activity location
            this.searchLocation(location, true).catch(() => {
                this.focusFallbackCoordinates(lat, lng, location);
            });
            return;
        }

        this.focusFallbackCoordinates(lat, lng, location);
    }

    focusFallbackCoordinates(lat, lng, label) {
        if (Number.isFinite(lat) && Number.isFinite(lng)) {
            this.setMarker(lat, lng, label, false);
            return;
        }

        this.map.setView(this.defaultCenter, 12);
    }

    selectedActivityOption() {
        return this.activitySelect.options[this.activitySelect.selectedIndex];
    }

    activityHasCoordinates(option) {
        return option
            && Number.isFinite(Number(option.dataset.latitude))
            && Number.isFinite(Number(option.dataset.longitude));
    }

    focusSelectedActivity(shouldUpdateInput) {
        const option = this.selectedActivityOption();

        if (!this.activityHasCoordinates(option)) {
            return;
        }

        const label = option.dataset.location || option.textContent.trim();
        this.setMarker(
            Number(option.dataset.latitude),
            Number(option.dataset.longitude),
            label,
            shouldUpdateInput
        );
    }

    setMarker(lat, lng, label, shouldUpdateInput) {
        const position = [lat, lng];

        if (this.marker) {
            this.marker.setLatLng(position);
        } else {
            this.marker = L.marker(position).addTo(this.map);
        }

        if (label) {
            this.marker.bindPopup(label).openPopup();
        }

        if (shouldUpdateInput && this.locationInput && label) {
            this.locationInput.value = label;
        }

        this.map.setView(position, 15);
    }

    async searchLocation(query, shouldUseAsLabel = false) {
        const response = await fetch(
            `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(query)}`
        );
        const results = await response.json();

        if (!results.length) {
            return;
        }

        this.setMarker(
            Number(results[0].lat),
            Number(results[0].lon),
            shouldUseAsLabel ? query : results[0].display_name,
            false
        );
    }

    async reverseGeocode(lat, lng) {
        const response = await fetch(
            `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}`
        );
        const result = await response.json();
        return result.display_name || this.formatCoordinates(lat, lng);
    }

    formatCoordinates(lat, lng) {
        return `${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    }
}

document.addEventListener("DOMContentLoaded", function () {
    const createMapElement = document.getElementById("travelgroup-location-map");

    // Create page: interactive map picker that updates the form
    if (createMapElement) {
        new TravelGroupMap({
            mapElement: createMapElement,
            locationInput: document.getElementById("location"),
            activitySelect: document.getElementById("activityId"),
            activityLocationButton: document.getElementById("useActivityLocation"),
            transportModeSelect: document.getElementById("mode"),
            transportModeIcon: document.getElementById("transportModeIcon"),
            transportModeButtons: document.querySelectorAll(".transport-mode-option")
        }).init();
    }

    const detailMapElement = document.getElementById("travelgroup-detail-map");

    // Detail page: read-only map, but still opens the same meeting point in Google Maps
    if (detailMapElement) {
        new TravelGroupMap({
            mapElement: detailMapElement,
            mapsLink: document.querySelector("[data-maps-query]"),
            readOnly: true
        }).init();
    }
});
