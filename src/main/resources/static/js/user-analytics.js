const userAnalyticsConfigElement = document.getElementById("userAnalyticsConfig");
const userAnalyticsConfig = userAnalyticsConfigElement
    ? JSON.parse(userAnalyticsConfigElement.textContent)
    : {};
const userAnalyticsI18n = userAnalyticsConfig.i18n || {};

new Chart(document.getElementById("personalCo2Chart"), {
    type: "bar",
    data: {
        labels: [userAnalyticsI18n.baseline, userAnalyticsI18n.actual],
        datasets: [{
            label: userAnalyticsI18n.co2Axis,
            data: [userAnalyticsConfig.personalBaseline || 0, userAnalyticsConfig.personalActual || 0],
            backgroundColor: ["#222222", "#2f8f5b"]
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {legend: {display: false}},
        scales: {y: {beginAtZero: true, title: {display: true, text: userAnalyticsI18n.co2Axis}}}
    }
});