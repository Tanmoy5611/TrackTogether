const API_REPORTS   = "/api/moderators/reports";
const API_MODERATORS = "/api/moderators/list";

const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

/* True when the logged-in user has ROLE_ADMIN (set via Thymeleaf data attribute). */
const isAdmin = document.body.dataset.isAdmin === "true";

function csrfHeaders() {
    return csrfToken && csrfHeader
        ? { [csrfHeader]: csrfToken }
        : {};
}

// Tab switching

function switchTab(tab) {
    const isCurrent = tab === "current";
    document.getElementById("tab-current").style.display  = isCurrent ? "" : "none";
    document.getElementById("tab-history").style.display  = isCurrent ? "none" : "";
    document.getElementById("tab-controls").style.display = isCurrent ? "" : "none";
    document.getElementById("tab-btn-current").style.fontWeight = isCurrent ? "bold" : "";
    document.getElementById("tab-btn-history").style.fontWeight = isCurrent ? "" : "bold";
}

// Load / render reports

async function loadReports() {
    const status = document.getElementById("statusFilter").value;
    let url;
    if (status === "PENDING") {
        url = `${API_REPORTS}?pending=true`;
    } else if (status) {
        url = `${API_REPORTS}?status=${encodeURIComponent(status)}`;
    } else {
        url = API_REPORTS;
    }
    const container = document.getElementById("tab-current");

    container.setAttribute("aria-busy", "true");
    renderCurrentState("loading");

    try {
        const res = await fetch(url);
        if (!res.ok) {
            renderCurrentState("error", `Server responded with HTTP ${res.status}.`);
            showToast(`Failed to fetch reports: HTTP ${res.status}`, "danger");
            return;
        }
        const data = await res.json();
        renderReports(data);
        updateSummaryCards(data);
    } catch (err) {
        renderCurrentState("error", "Could not load data. Use the Refresh button above to retry.");
        showToast(`Failed to fetch reports: ${err.message}`, "danger");
    }
}

function renderCurrentState(state, message = "") {
    const container = document.getElementById("tab-current");
    container.replaceChildren();
    container.setAttribute("aria-busy", state === "loading" ? "true" : "false");

    const empty = document.createElement("div");
    empty.className    = "kdg-empty";
    empty.style.gridColumn = "1 / -1";
    empty.style.textAlign  = "center";

    if (state === "loading") {
        const spinner = document.createElement("span");
        spinner.className = "spinner-border";
        spinner.setAttribute("aria-hidden", "true");

        const text = document.createElement("p");
        text.style.marginTop = "0.75rem";
        text.textContent     = "Loading reports...";

        empty.append(spinner, text);
    } else {
        const title = document.createElement("h2");
        title.textContent = state === "empty"
            ? "No reports found for the selected filter."
            : "Failed to load reports";

        const body = document.createElement("p");
        body.textContent = message || "Try changing the status filter or use the Refresh button above.";

        empty.append(title, body);
    }
    container.appendChild(empty);
}

function renderReports(reports) {
    const container = document.getElementById("tab-current");
    container.replaceChildren();
    container.setAttribute("aria-busy", "false");

    if (!reports.length) {
        renderCurrentState("empty");
        return;
    }

    reports.forEach((report, index) => container.appendChild(createReportCard(report, index + 1)));
}

function createReportCard(report, number) {
    const card = document.createElement("article");
    card.className = "kdg-group-card";
    card.id = `row-${report.reportId ?? ""}`;

    const body = document.createElement("div");
    body.className = "kdg-group-card__body";

    const title = document.createElement("h2");
    title.textContent = `Report #${number}`;

    const meta = document.createElement("dl");
    meta.className = "kdg-meta";
    meta.append(
        createMetaRow("bi-chat-left-text", "Reason",  report.reason     || "-"),
        createMetaRow("bi-clock",          "Created", report.createdAt  || "-"),
        createMetaRow("bi-flag",           "Status",  report.status     || "-"),
        createMetaRow("bi-person-badge",   "Assigned to",
            report.assignedModeratorName || "Unassigned")
    );

    const actions = document.createElement("div");
    actions.className = "kdg-actions";

    // "View details" link always visible
    const viewLink = document.createElement("a");
    viewLink.href      = `/moderator/reports/${report.reportId}`;
    viewLink.className = "kdg-button kdg-button--secondary";
    const viewIcon = document.createElement("i");
    viewIcon.className = "bi bi-eye";
    viewIcon.setAttribute("aria-hidden", "true");
    const viewLabel = document.createElement("span");
    viewLabel.textContent = "View details";
    viewLink.append(viewIcon, viewLabel);
    actions.appendChild(viewLink);

    appendReportActions(actions, report);

    body.append(title, meta, actions);
    card.appendChild(body);
    return card;
}

function createMetaRow(icon, label, value) {
    const row  = document.createElement("div");
    row.className = "kdg-meta__row";

    const term = document.createElement("dt");
    term.className = "kdg-meta__label";

    const iconEl = document.createElement("i");
    iconEl.className = `bi ${icon}`;
    iconEl.setAttribute("aria-hidden", "true");

    const labelText = document.createElement("span");
    labelText.textContent = label;

    const desc = document.createElement("dd");
    desc.className   = "kdg-meta__value";
    desc.textContent = value;

    term.append(iconEl, labelText);
    row.append(term, desc);
    return row;
}

function appendReportActions(container, report) {
    if (report.status === "OPEN") {
        container.appendChild(
            createActionButton("Claim", "bi-hand-index-thumb", () => claimReport(report.reportId))
        );

        // Admins additionally get an "Assign" button for OPEN reports
        if (isAdmin) {
            container.appendChild(
                createActionButton("Assign", "bi-person-plus", () => openAssignModal(report.reportId))
            );
        }
        return;
    }

    if (report.status === "REVIEWED") {
        container.appendChild(
            createActionButton("Resolve", "bi-check2-circle", () => setStatus(report.reportId, "RESOLVED"))
        );
        container.appendChild(
            createActionButton("Reject", "bi-x-circle", () => setStatus(report.reportId, "REJECTED"), true)
        );

        // Admins can re-assign a REVIEWED report too
        if (isAdmin) {
            container.appendChild(
                createActionButton("Reassign", "bi-person-plus", () => openAssignModal(report.reportId))
            );
        }
        return;
    }

    const dash = document.createElement("span");
    dash.textContent = "-";
    container.appendChild(dash);
}

function createActionButton(label, icon, onClick, danger = false) {
    const button = document.createElement("button");
    button.type      = "button";
    button.className = danger ? "kdg-button kdg-button--danger" : "kdg-button";

    const iconEl = document.createElement("i");
    iconEl.className = `bi ${icon}`;
    iconEl.setAttribute("aria-hidden", "true");

    const labelText = document.createElement("span");
    labelText.textContent = label;

    button.append(iconEl, labelText);
    button.addEventListener("click", onClick);
    return button;
}

// Claim / status update

async function claimReport(reportId) {
    try {
        const res = await fetch(`${API_REPORTS}/${reportId}/claim`, {
            method: "POST",
            headers: csrfHeaders()
        });

        if (!res.ok) {
            showToast(`Failed to claim report: HTTP ${res.status}`, "danger");
            return;
        }

        showToast("Report claimed successfully.", "success");
        loadReports();
        loadHistory();
    } catch (err) {
        showToast(`Failed to claim report: ${err.message}`, "danger");
    }
}

async function setStatus(reportId, status) {
    try {
        const res = await fetch(
            `${API_REPORTS}/${reportId}/status?status=${encodeURIComponent(status)}`,
            { method: "PATCH", headers: csrfHeaders() }
        );

        if (!res.ok) {
            showToast(`Failed to update status: HTTP ${res.status}`, "danger");
            return;
        }

        showToast(`Report marked as ${status}.`, "success");
        loadReports();
        loadHistory();
    } catch (err) {
        showToast(`Failed to update status: ${err.message}`, "danger");
    }
}

// Assign modal (admin only)

let _assignReportId = null;

async function openAssignModal(reportId) {
    _assignReportId = reportId;

    const modal  = document.getElementById("assign-modal");
    const select = document.getElementById("moderator-select");

    // Reset and show
    select.replaceChildren();
    const loading = document.createElement("option");
    loading.value       = "";
    loading.textContent = "Loading moderators…";
    select.appendChild(loading);
    modal.style.display = "flex";

    try {
        const res = await fetch(API_MODERATORS);
        if (!res.ok) {
            showToast(`Failed to load moderators: HTTP ${res.status}`, "danger");
            closeAssignModal();
            return;
        }

        const moderators = await res.json();
        select.replaceChildren();

        if (!moderators.length) {
            const empty = document.createElement("option");
            empty.value       = "";
            empty.textContent = "No moderators available";
            select.appendChild(empty);
            return;
        }

        const placeholder = document.createElement("option");
        placeholder.value       = "";
        placeholder.textContent = "— select a moderator —";
        select.appendChild(placeholder);

        moderators.forEach(mod => {
            const opt = document.createElement("option");
            opt.value       = mod.moderatorId;
            opt.textContent = mod.name;
            select.appendChild(opt);
        });
    } catch (err) {
        showToast(`Failed to load moderators: ${err.message}`, "danger");
        closeAssignModal();
    }
}

function closeAssignModal() {
    document.getElementById("assign-modal").style.display = "none";
    _assignReportId = null;
}

async function confirmAssign() {
    const moderatorId = document.getElementById("moderator-select").value;
    if (!moderatorId) {
        showToast("Please select a moderator.", "info");
        return;
    }

    try {
        const res = await fetch(
            `${API_REPORTS}/${_assignReportId}/assign?moderatorId=${encodeURIComponent(moderatorId)}`,
            { method: "POST", headers: csrfHeaders() }
        );

        if (!res.ok) {
            showToast(`Failed to assign report: HTTP ${res.status}`, "danger");
            return;
        }

        showToast("Report assigned successfully.", "success");
        closeAssignModal();
        loadReports();
        loadHistory();
    } catch (err) {
        showToast(`Failed to assign report: ${err.message}`, "danger");
    }
}

// History
async function loadHistory() {
    const container = document.getElementById("tab-history");

    try {
        const res = await fetch("/api/moderators/history");
        if (!res.ok) {
            renderHistoryMessage(`Failed to load history: HTTP ${res.status}`);
            return;
        }

        const entries = await res.json();

        if (!entries.length) {
            renderHistoryMessage("No history recorded yet.");
            return;
        }

        container.replaceChildren(createHistoryList(entries));
    } catch (err) {
        renderHistoryMessage(`Failed to load history: ${err.message}`);
    }
}

function renderHistoryMessage(message) {
    const container = document.getElementById("tab-history");
    const text = document.createElement("p");
    text.textContent = message;
    container.replaceChildren(text);
}

function createHistoryList(entries) {
    const list = document.createElement("ol");
    list.style.listStyle = "none";
    list.style.padding   = "0";
    list.style.margin    = "0";

    entries.forEach(entry => {
        const item = document.createElement("li");
        item.style.display      = "flex";
        item.style.gap          = "0.75rem";
        item.style.padding      = "0.6rem 0";
        item.style.borderBottom = "1px solid var(--kdg-border, #e5e7eb)";

        const icon = document.createElement("i");
        icon.className = "bi bi-arrow-right-circle";
        icon.setAttribute("aria-hidden", "true");
        icon.style.marginTop  = "0.15rem";
        icon.style.flexShrink = "0";

        const body   = document.createElement("div");
        const status = document.createElement("strong");
        status.textContent = `${entry.fromStatus ?? "-"} → ${entry.toStatus ?? "-"}`;

        const meta     = document.createElement("small");
        const changedAt = entry.changedAt
            ? entry.changedAt.replace("T", " ").substring(0, 16)
            : "-";
        meta.textContent = `${entry.changedByName ?? "System"} — ${changedAt}`;

        body.append(status, document.createElement("br"), meta);
        item.append(icon, body);
        list.appendChild(item);
    });

    return list;
}

// Summary cards

function updateSummaryCards(reports) {
    const counts = { OPEN: 0, REVIEWED: 0, RESOLVED: 0, REJECTED: 0 };
    reports.forEach(r => {
        if (counts[r.status] !== undefined) counts[r.status]++;
    });
    document.getElementById("cnt-open").textContent     = counts.OPEN;
    document.getElementById("cnt-reviewed").textContent = counts.REVIEWED;
    document.getElementById("cnt-resolved").textContent = counts.RESOLVED;
    document.getElementById("cnt-rejected").textContent = counts.REJECTED;
}

// Toast

const _TOAST_ICONS = {
    success: "bi-check-circle-fill",
    danger:  "bi-exclamation-triangle-fill",
    info:    "bi-info-circle-fill"
};

let _toastTimer = null;

function showToast(message, type = "info") {
    if (_toastTimer) { clearTimeout(_toastTimer); _toastTimer = null; }

    const container = document.getElementById("toast");

    const toast = document.createElement("div");
    toast.className = `kdg-toast kdg-toast--${type}`;
    toast.style.cssText = "opacity:0; transform:translateY(0.5rem); transition:opacity 0.22s ease, transform 0.22s ease;";

    const body = document.createElement("div");
    body.className = "toast-body";

    const icon = document.createElement("i");
    icon.className = `bi ${_TOAST_ICONS[type] ?? _TOAST_ICONS.info}`;
    icon.setAttribute("aria-hidden", "true");

    const text = document.createElement("span");
    text.textContent = message;

    const closeBtn = document.createElement("button");
    closeBtn.type = "button";
    closeBtn.setAttribute("aria-label", "Dismiss");
    closeBtn.style.cssText = "margin-left:auto; background:none; border:none; cursor:pointer; color:var(--kdg-muted); line-height:1; padding:0 0.1rem; flex-shrink:0;";
    const closeIcon = document.createElement("i");
    closeIcon.className = "bi bi-x-lg";
    closeIcon.setAttribute("aria-hidden", "true");
    closeBtn.appendChild(closeIcon);
    closeBtn.addEventListener("click", dismissToast);

    body.append(icon, text, closeBtn);
    toast.appendChild(body);
    container.replaceChildren(toast);

    requestAnimationFrame(() => requestAnimationFrame(() => {
        toast.style.opacity   = "1";
        toast.style.transform = "translateY(0)";
    }));

    _toastTimer = setTimeout(dismissToast, 5000);
}

function dismissToast() {
    if (_toastTimer) { clearTimeout(_toastTimer); _toastTimer = null; }
    const container = document.getElementById("toast");
    const toast = container?.firstElementChild;
    if (!toast) return;
    toast.style.opacity   = "0";
    toast.style.transform = "translateY(0.5rem)";
    setTimeout(() => container.replaceChildren(), 240);
}

//  Bootstrap

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("statusFilter").addEventListener("change", loadReports);
    document.getElementById("tab-btn-current").addEventListener("click", () => switchTab("current"));
    document.getElementById("tab-btn-history").addEventListener("click", () => switchTab("history"));
    document.getElementById("refreshReportsButton").addEventListener("click", loadReports);

    // Modal buttons
    document.getElementById("assign-cancel-btn").addEventListener("click", closeAssignModal);
    document.getElementById("assign-confirm-btn").addEventListener("click", confirmAssign);

    // Close modal when clicking outside the dialog panel
    document.getElementById("assign-modal").addEventListener("click", (e) => {
        if (e.target === e.currentTarget) closeAssignModal();
    });

    switchTab("current");
    loadReports();
    loadHistory();
});