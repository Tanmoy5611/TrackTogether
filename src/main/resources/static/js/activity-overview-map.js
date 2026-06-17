document.addEventListener("DOMContentLoaded", function () {
    const mapElement = document.getElementById("map");
    let lat = Number.parseFloat(mapElement?.dataset.latitude);
    let lng = Number.parseFloat(mapElement?.dataset.longitude);

    if (Number.isNaN(lat) || Number.isNaN(lng)) {
        lat = 51.0543;
        lng = 3.7174;
    }

    const map = L.map("map").setView([lat, lng], 14);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    L.marker([lat, lng]).addTo(map);
});