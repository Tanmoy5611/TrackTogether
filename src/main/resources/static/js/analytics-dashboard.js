const analyticsDashboardConfigElement = document.getElementById("analyticsDashboardConfig");
const analyticsDashboardConfig = analyticsDashboardConfigElement
    ? JSON.parse(analyticsDashboardConfigElement.textContent)
    : {};
const chartColors = ["#2f7fa3", "#2f8f5b", "#fff200", "#8a5cf6", "#f07a3f"];
const analyticsDashboardI18n = analyticsDashboardConfig.i18n || {};
let co2Chart;

async function loadCo2Chart(period) {
    const response = await fetch(`/analytics/dashboard/co2?period=${period}`);
    const data = await response.json();

    const labels = data.map(point => point.period);
    const baseline = data.map(point => point.baselineEmissionsKg);
    const actual = data.map(point => point.actualEmissionsKg);
    const tooltipData = Object.fromEntries(data.map(point => [point.period, point]));

    if (co2Chart) {
        co2Chart.destroy();
    }

    co2Chart = new Chart(document.getElementById("co2Chart"), {
        type: "line",
        data: {
            labels: labels,
            datasets: [
                {
                    label: analyticsDashboardI18n.baseline,
                    data: baseline,
                    borderColor: "#222222",
                    backgroundColor: "rgba(34, 34, 34, 0.08)",
                    tension: 0.28,
                    fill: true
                },
                {
                    label: analyticsDashboardI18n.actual,
                    data: actual,
                    borderColor: "#2f8f5b",
                    backgroundColor: "rgba(47, 143, 91, 0.12)",
                    tension: 0.28,
                    fill: true
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {mode: "index", intersect: false},
            scales: {
                y: {beginAtZero: true, title: {display: true, text: analyticsDashboardI18n.co2Axis}},
                x: {title: {display: true, text: analyticsDashboardI18n.timeAxis}}
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        afterBody: function (items) {
                            const point = tooltipData[items[0].label];
                            return [
                                `${analyticsDashboardI18n.events}: ${point.eventCount}`,
                                `${analyticsDashboardI18n.groupedUsers}: ${point.groupedUserCount}`,
                                `${analyticsDashboardI18n.savings}: ${point.savingsKg} kg`
                            ];
                        }
                    }
                }
            }
        }
    });
}

new Chart(document.getElementById("transportChart"), {
    type: "bar",
    data: {
        labels: analyticsDashboardConfig.transportLabels || [],
        datasets: [{
            label: analyticsDashboardI18n.participants,
            data: analyticsDashboardConfig.transportCounts || [],
            backgroundColor: chartColors
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {legend: {display: false}},
        scales: {y: {beginAtZero: true, ticks: {precision: 0}}}
    }
});

document.getElementById("periodSelect").addEventListener("change", event => loadCo2Chart(event.target.value));
loadCo2Chart("MONTHLY");