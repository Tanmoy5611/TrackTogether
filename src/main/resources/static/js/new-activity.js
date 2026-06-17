document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("form");
    const selectLocationMessage = form?.dataset.selectLocationMessage || "Please select a location.";
    const map = L.map("map").setView([51.0543, 3.7174], 13);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    let marker;

    const locationInput = document.getElementById("location");
    const latInput = document.getElementById("lat");
    const lngInput = document.getElementById("lng");
    const suggestionsBox = document.getElementById("suggestions");

    function setMarker(lat, lng) {
        latInput.value = lat;
        lngInput.value = lng;

        if (marker) {
            map.removeLayer(marker);
        }

        marker = L.marker([lat, lng]).addTo(map);
        map.setView([lat, lng], 14);
    }

    map.on("click", function (e) {
        setMarker(e.latlng.lat, e.latlng.lng);
    });

    let timeout;

    locationInput.addEventListener("input", function () {
        clearTimeout(timeout);

        const query = this.value;

        if (query.length < 3) {
            suggestionsBox.innerHTML = "";
            return;
        }

        timeout = setTimeout(async () => {
            const res = await fetch(
                `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}`
            );
            const data = await res.json();

            suggestionsBox.innerHTML = "";

            data.slice(0, 5).forEach(place => {
                const div = document.createElement("div");
                div.textContent = place.display_name;

                div.addEventListener("click", () => {
                    locationInput.value = place.display_name;
                    suggestionsBox.innerHTML = "";

                    setMarker(
                        parseFloat(place.lat),
                        parseFloat(place.lon)
                    );
                });

                suggestionsBox.appendChild(div);
            });
        }, 300);
    });

    document.addEventListener("click", function (e) {
        if (e.target !== locationInput) {
            suggestionsBox.innerHTML = "";
        }
    });

    form.addEventListener("submit", function (e) {
        if (!latInput.value || !lngInput.value) {
            e.preventDefault();
            alert(selectLocationMessage);
        }
    });

    const today = new Date().toISOString().split("T")[0];
    document.getElementById("date").setAttribute("min", today);
});