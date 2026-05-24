document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("travelgroup-location-share-form");
    const useLocationButton = document.getElementById("travelgroup-use-current-location");
    const addressInput = document.getElementById("sharedLocationAddress");
    const latitudeInput = document.getElementById("sharedLatitude");
    const longitudeInput = document.getElementById("sharedLongitude");
    const statusText = document.getElementById("travelgroup-location-share-status");
    const shareButton = document.getElementById("travelgroup-share-location-button");
    const removeLocationForm = document.getElementById("travelgroup-remove-location-form");

    if (!form || !useLocationButton || !addressInput || !latitudeInput || !longitudeInput || !statusText) {
        return;
    }

    const liveAction = form.dataset.liveAction || form.action;
    const csrfToken = document.querySelector("meta[name='_csrf']")?.content;
    const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.content;
    let watchId = null;
    let lastSharedPosition = null;
    let lastSharedAt = 0;
    let idleLiveLocationLabel = useLocationButton.dataset.idleLabel || "Live location";

    const setLocationSavedState = () => {
        shareButton?.remove();
        removeLocationForm?.classList.remove("travelgroup-location-share__remove--hidden");
        idleLiveLocationLabel = "Update live location";
        useLocationButton.dataset.idleLabel = idleLiveLocationLabel;
        addressInput.readOnly = true;
    };

    const setStatus = (message, statusClass) => {
        statusText.textContent = message;
        statusText.classList.remove(
            "travelgroup-location-share__hint--success",
            "travelgroup-location-share__hint--error"
        );

        if (statusClass) {
            statusText.classList.add(statusClass);
        }
    };

    const csrfHeaders = () => csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {};

    const distanceInMeters = (first, second) => {
        if (!first || !second) {
            return Number.POSITIVE_INFINITY;
        }

        const earthRadius = 6371000;
        const toRadians = (degrees) => degrees * Math.PI / 180;
        const latitudeDelta = toRadians(second.latitude - first.latitude);
        const longitudeDelta = toRadians(second.longitude - first.longitude);
        const firstLatitude = toRadians(first.latitude);
        const secondLatitude = toRadians(second.latitude);
        const a = Math.sin(latitudeDelta / 2) ** 2
            + Math.cos(firstLatitude) * Math.cos(secondLatitude) * Math.sin(longitudeDelta / 2) ** 2;

        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    };

    const updateLiveButton = (isLive) => {
        useLocationButton.classList.toggle("travelgroup-location-share__live-button--active", isLive);
        useLocationButton.setAttribute("aria-pressed", String(isLive));
        useLocationButton.querySelector(".bi")?.classList.toggle("bi-broadcast-pin", !isLive);
        useLocationButton.querySelector(".bi")?.classList.toggle("bi-stop-circle", isLive);
        useLocationButton.querySelector("span").textContent = isLive ? "Stop live location" : idleLiveLocationLabel;
    };

    const stopLiveLocation = (message, statusClass) => {
        if (watchId !== null) {
            navigator.geolocation.clearWatch(watchId);
            watchId = null;
        }

        updateLiveButton(false);

        if (message) {
            setStatus(message, statusClass);
        }
    };

    const postLiveLocation = async (position) => {
        const latitude = Number(position.coords.latitude.toFixed(6));
        const longitude = Number(position.coords.longitude.toFixed(6));
        const nextPosition = { latitude, longitude };
        const now = Date.now();

        // Browser GPS can fire repeatedly while standing still; save only real movement or periodic refreshes.
        if (distanceInMeters(lastSharedPosition, nextPosition) < 10 && now - lastSharedAt < 30000) {
            return;
        }

        latitudeInput.value = latitude.toFixed(6);
        longitudeInput.value = longitude.toFixed(6);
        addressInput.value = `Live location (${latitude.toFixed(6)}, ${longitude.toFixed(6)})`;

        const body = new URLSearchParams();
        body.set("address", addressInput.value);
        body.set("latitude", latitudeInput.value);
        body.set("longitude", longitudeInput.value);

        const response = await fetch(liveAction, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                "Accept": "application/json",
                ...csrfHeaders()
            },
            body
        });

        if (!response.ok) {
            throw new Error("Live location update failed");
        }

        lastSharedPosition = nextPosition;
        lastSharedAt = now;
        setLocationSavedState();
        setStatus(
            `Live location shared at ${latitude.toFixed(6)}, ${longitude.toFixed(6)}.`,
            "travelgroup-location-share__hint--success"
        );
    };

    useLocationButton.addEventListener("click", () => {
        if (!navigator.geolocation) {
            setStatus(
                "Your browser does not support location sharing. You can still type the address manually.",
                "travelgroup-location-share__hint--error"
            );
            return;
        }

        if (watchId !== null) {
            stopLiveLocation(
                "Live location sharing stopped. Your last shared location stays visible to the group.",
                null
            );
            return;
        }

        setStatus("Starting live location sharing...", null);
        updateLiveButton(true);

        watchId = navigator.geolocation.watchPosition(
            (position) => {
                postLiveLocation(position).catch(() => {
                    stopLiveLocation(
                        "Live location could not be saved. Check your connection and try again.",
                        "travelgroup-location-share__hint--error"
                    );
                });
            },
            () => {
                stopLiveLocation(
                    "Location permission was not granted. Type the address manually and save it.",
                    "travelgroup-location-share__hint--error"
                );
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 60000
            }
        );
    });

    // Live sharing is intentionally scoped to this page session; leaving the page stops browser tracking.
    window.addEventListener("beforeunload", () => {
        if (watchId !== null) {
            navigator.geolocation.clearWatch(watchId);
        }
    });
});