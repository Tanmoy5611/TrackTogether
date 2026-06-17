document.addEventListener("DOMContentLoaded", () => {
    const locationInput = document.getElementById("defaultDepartureLocation");
    const latitudeInput = document.getElementById("defaultLatitude");
    const longitudeInput = document.getElementById("defaultLongitude");
    const suggestionsList = document.getElementById("departureLocationSuggestions");

    let locationResults = [];
    let searchTimeout;

    if (locationInput) {
        locationInput.addEventListener("input", function () {
            const query = locationInput.value.trim();

            clearTimeout(searchTimeout);

            if (query.length < 1) {
                suggestionsList.innerHTML = "";
                locationResults = [];
                latitudeInput.value = "";
                longitudeInput.value = "";
                return;
            }

            searchTimeout = setTimeout(() => {
                fetchLocationSuggestions(query);
            }, 350);
        });

        locationInput.addEventListener("change", function () {
            const selectedLocation = locationResults.find(location =>
                location.display_name === locationInput.value
            );

            if (selectedLocation) {
                latitudeInput.value = selectedLocation.lat;
                longitudeInput.value = selectedLocation.lon;
            }
        });
    }

    async function fetchLocationSuggestions(query) {
        try {
            const response = await fetch(
                `https://nominatim.openstreetmap.org/search?format=json&limit=6&countrycodes=be&q=${encodeURIComponent(query)}`
            );

            if (!response.ok) {
                return;
            }

            locationResults = await response.json();
            suggestionsList.innerHTML = "";

            locationResults.forEach(location => {
                const option = document.createElement("option");
                option.value = location.display_name;
                suggestionsList.appendChild(option);
            });
        } catch (error) {
            console.error("Could not load location suggestions", error);
        }
    }
});