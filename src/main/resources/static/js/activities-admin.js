const activitySearch = document.getElementById("activitySearch");
const timingFilter = document.getElementById("timingFilter");
const activityTable = document.getElementById("activityTable");
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

let activitySearchTimeout = null;

if (activitySearch && timingFilter && activityTable) {
    bindActivityRows();
    bindVerificationButtons();
    bindRemoveButtons();

    activitySearch.addEventListener("input", () => {
        clearTimeout(activitySearchTimeout);
        activitySearchTimeout = setTimeout(fetchActivities, 300);
    });

    timingFilter.addEventListener("change", fetchActivities);
}

function fetchActivities() {
    const search = activitySearch.value;
    const timing = timingFilter.value;

    fetch(`/super_admin/api/activities?search=${encodeURIComponent(search)}&timing=${encodeURIComponent(timing)}`)
        .then(res => res.json())
        .then(activities => {
            activityTable.replaceChildren();

            if (activities.length === 0) {
                const row = document.createElement("tr");
                const cell = document.createElement("td");

                cell.colSpan = 8;
                cell.textContent = "No activities found";

                row.appendChild(cell);
                activityTable.appendChild(row);
                return;
            }

            activities.forEach(activity => {
                const row = document.createElement("tr");

                row.dataset.activityHref = `/activities/${activity.id}`;
                attachActivityRowClick(row);

                appendTextCell(row, activity.name);
                appendBadgeCell(row, activity.kdgActivity ? "KdG Activity" : "Community", activity.kdgActivity ? "text-bg-primary" : "text-bg-secondary");
                appendVerificationCell(row, activity);
                appendTextCell(row, activity.location);
                appendTextCell(row, activity.date);
                appendTextCell(row, activity.time);
                appendTextCell(row, activity.creatorName);
                appendActionsCell(row, activity);

                activityTable.appendChild(row);
            });
        });
}

function appendTextCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value ?? "";
    row.appendChild(cell);
}

function bindActivityRows() {
    document
        .querySelectorAll("[data-activity-href]")
        .forEach(attachActivityRowClick);
}

function attachActivityRowClick(row) {
    row.style.cursor = "pointer";
    row.addEventListener("click", () => {
        window.location.href = row.dataset.activityHref;
    });
}

function appendBadgeCell(row, value, badgeClass) {
    const cell = document.createElement("td");
    const badge = document.createElement("span");

    badge.className = `badge ${badgeClass}`;
    badge.textContent = value ?? "";

    cell.appendChild(badge);
    row.appendChild(cell);
}

function appendVerificationCell(row, activity) {
    const cell = document.createElement("td");

    const status = activity.verificationStatus ?? "PENDING";
    const badge = document.createElement("span");

    badge.className = `badge ${verificationBadgeClass(status)}`;
    badge.textContent = formatVerificationStatus(status);

    cell.appendChild(badge);
    row.appendChild(cell);
}

function formatVerificationStatus(status) {
    return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
}

function appendActionsCell(row, activity) {
    const cell = document.createElement("td");

    if (!activity.canVerify && !activity.canRemove) {
        cell.className = "text-muted";
        cell.textContent = "No action";
        row.appendChild(cell);
        return;
    }

    const actions = document.createElement("div");
    actions.className = "d-flex gap-2";

    if (activity.canVerify) {
        const approveButton = createVerificationButton(activity.id, "APPROVED", "Approve", "btn btn-sm btn-success");
        const disapproveButton = createVerificationButton(activity.id, "DISAPPROVED", "Disapprove", "btn btn-sm btn-outline-danger");

        actions.appendChild(approveButton);
        actions.appendChild(disapproveButton);
    }

    if (activity.canRemove) {
        actions.appendChild(createRemoveButton(activity.id));
    }

    cell.appendChild(actions);
    row.appendChild(cell);
}

function createVerificationButton(activityId, status, label, className) {
    const button = document.createElement("button");

    button.type = "button";
    button.className = `${className} activity-verification-button`;
    button.dataset.activityId = activityId;
    button.dataset.status = status;
    button.textContent = label;

    attachVerificationButton(button);

    return button;
}

function bindVerificationButtons() {
    document
        .querySelectorAll(".activity-verification-button")
        .forEach(attachVerificationButton);
}

function bindRemoveButtons() {
    document
        .querySelectorAll(".activity-remove-button")
        .forEach(attachRemoveButton);
}

function attachVerificationButton(button) {
    button.addEventListener("click", event => {
        event.preventDefault();
        event.stopPropagation();
        updateActivityVerification(button.dataset.activityId, button.dataset.status);
    });
}

function updateActivityVerification(activityId, status) {
    const headers = {};

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    fetch(`/super_admin/api/activities/${activityId}/verification?status=${encodeURIComponent(status)}`, {
        method: "PATCH",
        headers
    })
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            return res.json();
        })
        .then(fetchActivities)
        .catch(() => {
            alert("Failed to update activity verification status");
        });
}

function createRemoveButton(activityId) {
    const button = document.createElement("button");

    button.type = "button";
    button.className = "btn btn-sm btn-outline-secondary activity-remove-button";
    button.dataset.activityId = activityId;
    button.textContent = "Remove";

    attachRemoveButton(button);

    return button;
}

function attachRemoveButton(button) {
    button.addEventListener("click", event => {
        event.preventDefault();
        event.stopPropagation();
        removeActivity(button.dataset.activityId);
    });
}

function removeActivity(activityId) {
    if (!confirm("Remove this activity?")) {
        return;
    }

    const headers = {};

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    fetch(`/super_admin/api/activities/${activityId}`, {
        method: "DELETE",
        headers
    })
        .then(res => {
            if (!res.ok) {
                throw new Error(`HTTP ${res.status}`);
            }
            fetchActivities();
        })
        .catch(() => {
            alert("Failed to remove activity");
        });
}

function verificationBadgeClass(status) {
    if (status === "APPROVED") {
        return "text-bg-success";
    }

    if (status === "DISAPPROVED") {
        return "text-bg-danger";
    }

    return "text-bg-warning";
}