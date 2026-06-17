// Report Message

const REPORT_CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
const REPORT_CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

let _reportTargetMessageId = null;

function openReportModal(messageId) {
    _reportTargetMessageId = messageId;
    const textarea = document.getElementById("report-reason-input");
    if (textarea) textarea.value = "";
    const modal = document.getElementById("report-modal");
    if (modal) {
        modal.style.display = "flex";
        textarea?.focus();
    }
}

function closeReportModal() {
    const modal = document.getElementById("report-modal");
    if (modal) modal.style.display = "none";
    _reportTargetMessageId = null;
}

async function submitReport() {
    const reason = document.getElementById("report-reason-input")?.value.trim();

    if (!reason) {
        showReportToast("Please provide a reason before submitting.");
        return;
    }

    const submitBtn = document.getElementById("report-submit-btn");
    if (submitBtn) submitBtn.disabled = true;

    try {
        const headers = { "Content-Type": "application/json" };
        if (REPORT_CSRF_TOKEN && REPORT_CSRF_HEADER) {
            headers[REPORT_CSRF_HEADER] = REPORT_CSRF_TOKEN;
        }

        const res = await fetch(`/api/messages/${_reportTargetMessageId}/reports`, {
            method: "POST",
            headers,
            body: JSON.stringify({ reason })
        });

        if (res.status === 201) {
            closeReportModal();
            showReportToast("Report submitted — a moderator will review it. Thank you.");
        } else if (res.status === 400) {
            const body = await res.json().catch(() => null);
            showReportToast(body?.message || "Could not submit report. Please try again.");
        } else if (res.status === 401) {
            showReportToast("You must be logged in to report a message.");
        } else {
            showReportToast(`Failed to submit report (HTTP ${res.status}).`);
        }
    } catch (e) {
        showReportToast("Network error — please check your connection and try again.");
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
}

function showReportToast(message) {
    document.querySelectorAll(".chat-report-toast").forEach(t => t.remove());

    const toast = document.createElement("div");
    toast.className   = "chat-report-toast";
    toast.textContent = message;
    toast.setAttribute("role", "status");
    toast.setAttribute("aria-live", "polite");
    document.body.appendChild(toast);

    setTimeout(() => toast.remove(), 4500);
}

// ── Bootstrap ────────────────────────────────────────────────────────────────

document.addEventListener("DOMContentLoaded", () => {
    const modal     = document.getElementById("report-modal");
    const cancelBtn = document.getElementById("report-cancel-btn");
    const submitBtn = document.getElementById("report-submit-btn");

    if (!modal) return; // modal not present on this page

    cancelBtn?.addEventListener("click", closeReportModal);
    submitBtn?.addEventListener("click", submitReport);

    // Close when clicking the backdrop overlay
    modal.addEventListener("click", (e) => {
        if (e.target === e.currentTarget) closeReportModal();
    });

    document.addEventListener("click", (e) => {
        const reportButton = e.target.closest(".chat-message-report-btn");
        if (reportButton) {
            openReportModal(reportButton.dataset.messageId);
        }
    });

    // Keyboard: Escape closes the modal
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") closeReportModal();
    });
});