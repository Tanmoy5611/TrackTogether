const reportId   = document.body.dataset.reportId;
const isAdmin    = document.body.dataset.isAdmin === "true";

const csrfToken  = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

const API_BASE       = `/api/moderators/reports/${reportId}`;
const API_MODERATORS = "/api/moderators/list";

function csrfHeaders() {
    return csrfToken && csrfHeader
        ? { [csrfHeader]: csrfToken }
        : {};
}

// Fetch and render report

async function loadReport() {
    try {
        const res = await fetch(API_BASE);
        if (res.status === 404) {
            showError("This report does not exist or has been removed.");
            return;
        }
        if (!res.ok) {
            showError(`Server responded with HTTP ${res.status}.`);
            return;
        }
        const report = await res.json();
        renderReport(report);
        renderActions(report);
        loadHistory();
        loadChatContext();
    } catch (err) {
        showError(`Could not load report: ${err.message}`);
    }
}

function renderReport(r) {
    // Header
    document.getElementById("report-title").textContent = "Report Detail";
    document.getElementById("report-subtitle").textContent =
        `Status: ${formatStatus(r.status)} · Opened: ${r.createdAt ?? "–"}`;

    // Reporter
    document.getElementById("reporter-name").textContent  = r.reporterName  || "–";
    document.getElementById("reporter-email").textContent = r.reporterEmail || "–";

    // Reported
    document.getElementById("reported-name").textContent  = r.reportedName  || "–";
    document.getElementById("reported-email").textContent = r.reportedEmail || "–";

    // Flagged message
    const msgContent = document.getElementById("message-content");
    msgContent.textContent = r.messageContent || "(message content unavailable)";

    const msgMeta = document.getElementById("message-meta");
    if (r.reportedName && r.messageTimestamp) {
        const ts = r.messageTimestamp.replace("T", " ").substring(0, 16);
        msgMeta.textContent = `Sent by ${r.reportedName} on ${ts}`;
    } else if (r.messageTimestamp) {
        msgMeta.textContent = `Sent on ${r.messageTimestamp.replace("T", " ").substring(0, 16)}`;
    } else {
        msgMeta.textContent = "";
    }

    // Details
    document.getElementById("detail-reason").textContent    = r.reason    || "–";
    document.getElementById("detail-created").textContent   = r.createdAt || "–";
    document.getElementById("detail-status").textContent    = formatStatus(r.status);
    document.getElementById("detail-moderator").textContent =
        r.assignedModeratorName || "Unassigned";

    // Show content, hide skeleton
    document.getElementById("report-loading").style.display  = "none";
    document.getElementById("report-content").style.display  = "";
}

// Action buttons

function renderActions(report) {
    const panel   = document.getElementById("actions-panel");
    const buttons = document.getElementById("action-buttons");
    buttons.replaceChildren();

    if (report.status === "OPEN") {
        buttons.appendChild(makeButton("Claim report",    "bi-hand-index-thumb", () => claimReport()));
        if (isAdmin) {
            buttons.appendChild(makeButton("Assign to moderator", "bi-person-plus", () => openAssignModal()));
        }
        panel.style.display = "";
        return;
    }

    if (report.status === "REVIEWED") {
        buttons.appendChild(makeButton("Mark as Resolved", "bi-check2-circle", () => setStatus("RESOLVED")));
        buttons.appendChild(makeButton("Reject report",    "bi-x-circle",      () => setStatus("REJECTED"), true));
        if (isAdmin) {
            buttons.appendChild(makeButton("Reassign", "bi-person-plus", () => openAssignModal()));
        }
        panel.style.display = "";
        return;
    }

    // RESOLVED / REJECTED — no further actions
    panel.style.display = "none";
}

function makeButton(label, icon, onClick, danger = false) {
    const btn = document.createElement("button");
    btn.type      = "button";
    btn.className = danger ? "kdg-button kdg-button--danger" : "kdg-button";

    const ico = document.createElement("i");
    ico.className = `bi ${icon}`;
    ico.setAttribute("aria-hidden", "true");

    const lbl = document.createElement("span");
    lbl.textContent = label;

    btn.append(ico, lbl);
    btn.addEventListener("click", onClick);
    return btn;
}

// Claim
async function claimReport() {
    try {
        const res = await fetch(`${API_BASE}/claim`, {
            method: "POST",
            headers: csrfHeaders()
        });
        if (!res.ok) {
            showToast(`Failed to claim report: HTTP ${res.status}`);
            return;
        }
        showToast("Report claimed and marked as Reviewed.");
        loadReport();
    } catch (err) {
        showToast(`Failed to claim report: ${err.message}`);
    }
}

// Status update

async function setStatus(status) {
    try {
        const res = await fetch(
            `${API_BASE}/status?status=${encodeURIComponent(status)}`,
            { method: "PATCH", headers: csrfHeaders() }
        );
        if (!res.ok) {
            showToast(`Failed to update status: HTTP ${res.status}`);
            return;
        }
        showToast(`Report marked as ${formatStatus(status)}.`);
        loadReport();
    } catch (err) {
        showToast(`Failed to update status: ${err.message}`);
    }
}

//  Assign modal

async function openAssignModal() {
    const modal  = document.getElementById("assign-modal");
    const select = document.getElementById("moderator-select");

    select.replaceChildren();
    const loading = document.createElement("option");
    loading.value       = "";
    loading.textContent = "Loading moderators…";
    select.appendChild(loading);
    modal.style.display = "flex";

    try {
        const res = await fetch(API_MODERATORS);
        if (!res.ok) {
            showToast(`Failed to load moderators: HTTP ${res.status}`);
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
        showToast(`Failed to load moderators: ${err.message}`);
        closeAssignModal();
    }
}

function closeAssignModal() {
    document.getElementById("assign-modal").style.display = "none";
}

async function confirmAssign() {
    const moderatorId = document.getElementById("moderator-select").value;
    if (!moderatorId) {
        showToast("Please select a moderator.");
        return;
    }
    try {
        const res = await fetch(
            `${API_BASE}/assign?moderatorId=${encodeURIComponent(moderatorId)}`,
            { method: "POST", headers: csrfHeaders() }
        );
        if (!res.ok) {
            showToast(`Failed to assign report: HTTP ${res.status}`);
            return;
        }
        showToast("Report assigned successfully.");
        closeAssignModal();
        loadReport();
    } catch (err) {
        showToast(`Failed to assign report: ${err.message}`);
    }
}

// Chat context

async function loadChatContext() {
    const loading = document.getElementById("context-loading");
    const list    = document.getElementById("context-list");
    const empty   = document.getElementById("context-empty");

    try {
        const res = await fetch(`${API_BASE}/context`);
        loading.style.display = "none";

        if (!res.ok) {
            empty.style.display   = "";
            empty.textContent     = `Could not load context: HTTP ${res.status}`;
            return;
        }

        const messages = await res.json();

        if (!messages.length) {
            empty.style.display = "";
            return;
        }

        list.replaceChildren();
        messages.forEach(msg => list.appendChild(createContextBubble(msg)));
        list.style.display = "";
    } catch (err) {
        loading.style.display = "none";
        empty.style.display   = "";
        empty.textContent     = `Could not load context: ${err.message}`;
    }
}

function createContextBubble(msg) {
    const isFlagged = msg.flagged;

    const wrap = document.createElement("div");
    wrap.style.cssText = [
        "display:flex; flex-direction:column; padding:.6rem .8rem; margin-bottom:.4rem;",
        "border-radius:.5rem;",
        isFlagged
            ? "background:#fee2e2; border:2px solid #ef4444;"
            : "background:var(--kdg-surface-2,#f3f4f6); border:1px solid transparent;"
    ].join("");

    if (isFlagged) {
        const badge = document.createElement("span");
        badge.textContent = "⚑ Reported message";
        badge.style.cssText =
            "font-size:.72rem; font-weight:700; color:#ef4444; letter-spacing:.03em; margin-bottom:.3rem;";
        wrap.appendChild(badge);
    }

    const header = document.createElement("div");
    header.style.cssText =
        "display:flex; justify-content:space-between; align-items:baseline; margin-bottom:.2rem;";

    const sender = document.createElement("strong");
    sender.textContent  = msg.senderName || "Unknown";
    sender.style.fontSize = ".875rem";

    const time = document.createElement("small");
    time.textContent  = msg.timestamp
        ? msg.timestamp.replace("T", " ").substring(0, 16)
        : "";
    time.style.color  = "var(--kdg-muted,#6b7280)";
    time.style.marginLeft = ".75rem";
    time.style.whiteSpace = "nowrap";

    header.append(sender, time);

    const content = document.createElement("p");
    content.textContent  = msg.content || "";
    content.style.margin = "0";
    content.style.wordBreak = "break-word";

    wrap.append(header, content);
    return wrap;
}

// History
async function loadHistory() {
    const list    = document.getElementById("history-list");
    const loading = document.getElementById("history-loading");
    const empty   = document.getElementById("history-empty");

    try {
        const res = await fetch(`${API_BASE}/history`);
        if (!res.ok) {
            loading.style.display = "none";
            empty.style.display   = "";
            empty.textContent     = `Failed to load history: HTTP ${res.status}`;
            return;
        }
        const entries = await res.json();
        loading.style.display = "none";

        if (!entries.length) {
            empty.style.display = "";
            return;
        }

        list.replaceChildren();
        entries.forEach(entry => list.appendChild(createHistoryItem(entry)));
        list.style.display = "";
    } catch (err) {
        loading.style.display = "none";
        empty.style.display   = "";
        empty.textContent     = `Failed to load history: ${err.message}`;
    }
}

function createHistoryItem(entry) {
    const item = document.createElement("li");
    item.style.cssText =
        "display:flex; gap:.75rem; padding:.6rem 0; border-bottom:1px solid var(--kdg-border,#e5e7eb);";

    const icon = document.createElement("i");
    icon.className = "bi bi-arrow-right-circle";
    icon.setAttribute("aria-hidden", "true");
    icon.style.cssText = "margin-top:.15rem; flex-shrink:0;";

    const body   = document.createElement("div");
    const status = document.createElement("strong");
    status.textContent = `${entry.fromStatus ?? "–"} → ${entry.toStatus ?? "–"}`;

    const meta    = document.createElement("small");
    const changedAt = entry.changedAt
        ? entry.changedAt.replace("T", " ").substring(0, 16)
        : "–";
    meta.textContent = `${entry.changedByName ?? "System"} — ${changedAt}`;
    meta.style.color = "var(--kdg-muted,#6b7280)";

    body.append(status, document.createElement("br"), meta);
    item.append(icon, body);
    return item;
}

//  Error / toast helpers
function showError(message) {
    document.getElementById("report-loading").style.display = "none";
    document.getElementById("report-content").style.display = "none";
    document.getElementById("error-message").textContent    = message;
    document.getElementById("report-error").style.display   = "";
}

function showToast(message) {
    const toast = document.getElementById("toast");
    toast.textContent   = message;
    toast.style.display = "block";
    setTimeout(() => { toast.style.display = "none"; }, 4000);
}

function formatStatus(status) {
    if (!status) return "–";
    return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
}

//  Bootstrap

document.addEventListener("DOMContentLoaded", () => {
    if (!reportId) {
        showError("No report ID found in the page.");
        return;
    }

    document.getElementById("assign-cancel-btn").addEventListener("click", closeAssignModal);
    document.getElementById("assign-confirm-btn").addEventListener("click", confirmAssign);
    document.getElementById("assign-modal").addEventListener("click", e => {
        if (e.target === e.currentTarget) closeAssignModal();
    });

    loadReport();
});