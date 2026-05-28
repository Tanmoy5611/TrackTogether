document.addEventListener("DOMContentLoaded", () => {

    const toastElement = document.getElementById("actionToast");
    const toastMessage = document.getElementById("toastMessage");
    const csrfToken = document.querySelector("meta[name='_csrf']")?.content;
    const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.content;
    const pageLabels = document.querySelector("main.kdg-main")?.dataset ?? {};
    const pageLanguage = document.documentElement.lang || "en-GB";
    const pinnedGroupsStorageKey = "trackTogetherPinnedTravelGroups";

    const toast = window.bootstrap && toastElement
        ? new bootstrap.Toast(toastElement)
        : null;

    function csrfHeaders() {
        return csrfToken && csrfHeader
            ? {[csrfHeader]: csrfToken}
            : {};
    }

    function showToast(message) {
        if (toast && toastMessage) {
            toastMessage.textContent = message;
            toast.show();
            return;
        }

        window.alert(message);
    }

    function label(name, fallback) {
        return pageLabels[name] || fallback;
    }

    function pinnedGroupIds() {
        try {
            const storedValue = JSON.parse(localStorage.getItem(pinnedGroupsStorageKey) || "[]");
            return new Set(Array.isArray(storedValue) ? storedValue.map(String) : []);
        } catch (_) {
            return new Set();
        }
    }

    function savePinnedGroupIds(groupIds) {
        localStorage.setItem(pinnedGroupsStorageKey, JSON.stringify([...groupIds]));
    }

    function setPinButtonState(button, pinned) {
        const icon = button.querySelector(".bi");
        button.classList.toggle("is-pinned", pinned);
        button.setAttribute("aria-pressed", String(pinned));
        button.setAttribute("aria-label", pinned ? "Unpin group" : "Pin group");
        button.setAttribute("title", pinned ? "Unpin group" : "Pin group");

        if (icon) {
            icon.classList.toggle("bi-pin-angle-fill", pinned);
            icon.classList.toggle("bi-pin-angle", !pinned);
        }
    }

    function sortPinnedCards(grid, pinnedIds) {
        const cards = Array.from(grid.querySelectorAll(":scope > .kdg-group-card"));
        cards.forEach((card, index) => {
            if (!card.dataset.pinOriginalIndex) {
                card.dataset.pinOriginalIndex = String(index);
            }
        });

        cards
            .sort((first, second) => {
                const firstPinned = pinnedIds.has(String(first.dataset.groupId));
                const secondPinned = pinnedIds.has(String(second.dataset.groupId));
                if (firstPinned !== secondPinned) {
                    return firstPinned ? -1 : 1;
                }

                return Number(first.dataset.pinOriginalIndex) - Number(second.dataset.pinOriginalIndex);
            })
            .forEach(card => grid.appendChild(card));
    }

    function refreshPinnedCards(root = document) {
        const pinnedIds = pinnedGroupIds();

        root.querySelectorAll("[data-travelgroup-pin]").forEach(button => {
            const card = button.closest(".kdg-group-card");
            if (!card?.dataset.groupId) {
                return;
            }

            setPinButtonState(button, pinnedIds.has(String(card.dataset.groupId)));
        });

        root.querySelectorAll(".travelgroup-list-section .kdg-grid").forEach(grid => {
            sortPinnedCards(grid, pinnedIds);
        });
    }

    document.addEventListener("click", event => {
        const button = event.target.closest("[data-travelgroup-pin]");
        if (!button) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        const card = button.closest(".kdg-group-card");
        const groupId = card?.dataset.groupId;
        if (!groupId) {
            return;
        }

        const pinnedIds = pinnedGroupIds();
        if (pinnedIds.has(groupId)) {
            pinnedIds.delete(groupId);
        } else {
            pinnedIds.add(groupId);
        }

        savePinnedGroupIds(pinnedIds);
        refreshPinnedCards();
    });

    function setLoading(button, loading) {

        const text = button.querySelector(".btn-text");
        const spinner = button.querySelector(".spinner-border");

        if (loading) {
            button.disabled = true;
            text.classList.add("d-none");
            spinner.classList.remove("d-none");
        } else {
            button.disabled = false;
            text.classList.remove("d-none");
            spinner.classList.add("d-none");
        }
    }

    // JOIN
    document.querySelectorAll(".join-btn").forEach(button => {

        button.addEventListener("click", async () => {

            const groupId = button.dataset.groupId;

            if (!groupId) {
                showToast(label("joinPageError", "Open this page through Spring Boot to join a travel group."));
                return;
            }

            setLoading(button, true);

            try {

                const response = await fetch(
                    `/api/travelgroups/${groupId}/join`,
                    {
                        method: "POST",
                        headers: csrfHeaders()
                    }
                );

                if (!response.ok) {
                    showToast(label("couldNotJoin", "Could not join group"));
                    return;
                }

                showToast(label("joinedSuccessfully", "Joined successfully"));

            } catch (error) {

                showToast(error.message);

            } finally {

                setLoading(button, false);

            }

        });

    });

    // SUGGESTIONS
    async function loadSuggestions() {
        try {
            const response = await fetch("/api/travelgroups/suggestions");
            if (!response.ok) return;

            const suggestions = await response.json();
            if (!suggestions.length) return;

            renderSuggestions(suggestions);
        } catch (_) {
            // Suggestions are non-critical — fail silently
        }
    }

    function renderSuggestions(suggestions) {
        const section = document.getElementById("suggestions-section");
        const grid = document.getElementById("suggestions-grid");
        const countEl = document.getElementById("suggestions-count");

        if (!section || !grid) return;

        const suggestionLabels = section.dataset;
        countEl.textContent = formatCount(suggestionLabels.countTemplate, suggestions.length);
        grid.innerHTML = suggestions.map(suggestion => buildSuggestionCard(suggestion, suggestionLabels)).join("");
        section.hidden = false;

        grid.querySelectorAll(".suggestion-join-btn").forEach(btn => {
            btn.addEventListener("click", () => joinFromSuggestion(btn));
        });

        refreshPinnedCards(section);
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function formatCount(template, count) {
        return (template || "__count__ groups").replace("__count__", count);
    }

    function transportLabel(transportMode, labels) {
        const transportLabels = {
            CAR: labels.transportCar,
            CARPOOL: labels.transportCarpool,
            BIKE: labels.transportBike,
            WALK: labels.transportWalk,
            PUBLIC_TRANSPORT: labels.transportPublicTransport
        };

        return transportLabels[transportMode]
            || transportMode.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
    }

    function buildSuggestionCard(s, labels) {
        const time = s.departureTime
            ? new Date(s.departureTime).toLocaleString(pageLanguage, { dateStyle: "medium", timeStyle: "short" })
            : labels.notSetLabel || "-";
        const transport = s.transportMode
            ? transportLabel(s.transportMode, labels)
            : labels.notSetLabel || "-";
        const reasons = s.matchReasons
            .map(r => `<span class="travelgroup-status"><i class="bi bi-check" aria-hidden="true"></i> ${escapeHtml(r)}</span>`)
            .join("");
        const groupId = encodeURIComponent(s.groupId);
        const activityName = escapeHtml(s.activityName ?? labels.notSetLabel ?? "-");
        const departureLocation = escapeHtml(s.departureLocation ?? labels.notSetLabel ?? "-");

        return `
            <article class="kdg-group-card" id="suggestion-card-${groupId}" data-group-id="${escapeHtml(s.groupId)}">
                <button class="travelgroup-pin-button"
                        type="button"
                        title="Pin group"
                        aria-label="Pin group"
                        data-travelgroup-pin>
                    <i class="bi bi-pin-angle" aria-hidden="true"></i>
                </button>
                <div class="kdg-group-card__body">
                    <h2>${activityName}</h2>
                    <div class="travelgroup-card-status-block">${reasons}</div>
                    <dl class="kdg-meta">
                        <div class="kdg-meta__row">
                            <dt class="kdg-meta__label">
                                <i class="bi bi-geo-alt" aria-hidden="true"></i>
                                <span>${escapeHtml(labels.departureLabel || "Departure")}</span>
                            </dt>
                            <dd class="kdg-meta__value">${departureLocation}</dd>
                        </div>
                        <div class="kdg-meta__row">
                            <dt class="kdg-meta__label">
                                <i class="bi bi-clock" aria-hidden="true"></i>
                                <span>${escapeHtml(labels.timeLabel || "Time")}</span>
                            </dt>
                            <dd class="kdg-meta__value">${time}</dd>
                        </div>
                        <div class="kdg-meta__row">
                            <dt class="kdg-meta__label">
                                <i class="bi bi-signpost-split" aria-hidden="true"></i>
                                <span>${escapeHtml(labels.transportLabel || "Transport")}</span>
                            </dt>
                            <dd class="kdg-meta__value kdg-meta__value--transport">${escapeHtml(transport)}</dd>
                        </div>
                        <div class="kdg-meta__row">
                            <dt class="kdg-meta__label">
                                <i class="bi bi-people" aria-hidden="true"></i>
                                <span>${escapeHtml(labels.capacityLabel || "Capacity")}</span>
                            </dt>
                            <dd class="kdg-meta__value"
                                id="suggestion-count-${s.groupId}">${s.currentMemberCount} / ${s.maxMembers}</dd>
                        </div>
                    </dl>
                    <div class="kdg-actions kdg-card-actions travelgroups-card-actions">
                        <a class="kdg-button kdg-button--secondary"
                           href="/travelgroups/${groupId}">
                            <i class="bi bi-eye" aria-hidden="true"></i>
                            <span>${escapeHtml(labels.viewDetailsLabel || "View details")}</span>
                        </a>
                        <button class="kdg-button kdg-button--success suggestion-join-btn"
                                data-group-id="${escapeHtml(s.groupId)}">
                            <i class="bi bi-person-check" aria-hidden="true"></i>
                            ${escapeHtml(labels.iAmGoingTooLabel || "I'm going too")}
                        </button>
                    </div>
                </div>
            </article>`;
    }

    async function joinFromSuggestion(btn) {
        const groupId = btn.dataset.groupId;
        const originalHtml = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = `<i class="bi bi-hourglass-split" aria-hidden="true"></i> ${escapeHtml(label("joining", "Joining..."))}`;

        const restoreBtn = () => {
            btn.disabled = false;
            btn.innerHTML = originalHtml;
        };

        let response;
        try {
            response = await fetch(`/api/travelgroups/${groupId}/join`, {
                method: "POST",
                headers: csrfHeaders()
            });
        } catch (_) {
            showToast(label("couldNotReachServer", "Could not reach the server"));
            restoreBtn();
            return;
        }

        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            showToast(body.message || label("couldNotJoin", "Could not join group"));
            restoreBtn();
            return;
        }

        const result = await response.json();

        const countEl = document.getElementById(`suggestion-count-${groupId}`);
        if (countEl) {
            countEl.textContent = `${result.memberCount} / ${result.maxMembers}`;
        }

        const statusClass = result.pendingApproval
            ? "travelgroup-status travelgroup-status--pending"
            : "travelgroup-status travelgroup-status--joined";
        const iconClass = result.pendingApproval
            ? "bi-hourglass-split"
            : "bi-check-circle";
        const statusText = result.message || (result.pendingApproval
            ? label("requestSent", "Request sent")
            : label("youreGoingToo", "You're going too!"));

        btn.outerHTML = `
            <span class="${statusClass}">
                <i class="bi ${iconClass}" aria-hidden="true"></i>
                <span>${escapeHtml(statusText)}</span>
            </span>`;

        showToast(statusText);
    }

    loadSuggestions();
    refreshPinnedCards();


    // LEAVE
    document.querySelectorAll(".leave-btn").forEach(button => {

        button.addEventListener("click", async () => {

            const groupId = button.dataset.groupId;

            if (!groupId) {
                showToast(label("leavePageError", "Open this page through Spring Boot to leave a travel group."));
                return;
            }

            setLoading(button, true);

            try {

                const response = await fetch(
                    `/api/travelgroups/${groupId}/leave`,
                    {
                        method: "DELETE",
                        headers: csrfHeaders()
                    }
                );

                if (!response.ok) {
                    showToast(label("couldNotLeave", "Could not leave group"));
                    return;
                }

                // Remove card from UI
                const card = document.getElementById(`group-card-${groupId}`);
                if (card) {
                    card.remove();
                }

                showToast(label("leftTravelGroup", "Left travel group"));

            } catch (error) {

                showToast(error.message);

            } finally {

                setLoading(button, false);

            }

        });

    });

});