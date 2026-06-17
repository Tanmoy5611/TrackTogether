const statusSelect = document.getElementById("statusSelect");
const roleSelect = document.getElementById("roleSelect");
const userIdInput = document.getElementById("userId");
const userManagementError = document.getElementById("userManagementError");

const confirmModalEl = document.getElementById("confirmModal");
const confirmMessage = document.getElementById("confirmMessage");
const confirmBtn = document.getElementById("confirmBtn");

if (statusSelect && roleSelect && userIdInput && confirmModalEl && confirmMessage && confirmBtn) {

    const userId = userIdInput.value;
    const lastSuperAdmin = roleSelect.dataset.lastSuperAdmin === "true";

    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;

    const modal = new bootstrap.Modal(confirmModalEl);

    let previousStatus = statusSelect.value;
    let previousRole = roleSelect.value;

    let pendingChange = null;

    function csrfHeader() {
        return token && header
            ? { [header]: token }
            : {};
    }

    function clearPageError() {
        if (!userManagementError) return;

        userManagementError.classList.add("d-none");
        userManagementError.innerText = "";
    }

    function showPageError(message) {
        if (!userManagementError) {
            showToast(message, true);
            return;
        }

        userManagementError.innerText = message;
        userManagementError.classList.remove("d-none");
        userManagementError.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    function openConfirm(field) {
        const newStatus = statusSelect.value === "true";
        const newRole = roleSelect.value;

        if (field === "role"
            && lastSuperAdmin
            && previousRole === "SUPER_ADMIN"
            && newRole !== "SUPER_ADMIN") {
            roleSelect.value = previousRole;
            showPageError("The last super admin must keep the Super Admin role.");
            return;
        }

        clearPageError();

        confirmMessage.innerText =
            field === "status"
                ? `Change status from "${previousStatus === "true" ? "Active" : "Inactive"}" to "${newStatus ? "Active" : "Inactive"}"?`
                : `Change role from "${previousRole}" to "${newRole}"?`;

        pendingChange = {
            status: newStatus,
            role: newRole
        };

        if (newRole === "SUPER_ADMIN") {
            confirmBtn.classList.remove("btn-primary");
            confirmBtn.classList.add("btn-danger");
        } else {
            confirmBtn.classList.remove("btn-danger");
            confirmBtn.classList.add("btn-primary");
        }

        modal.show();
    }

    function applyChange() {
        if (!pendingChange) return;

        if (pendingChange.role === previousRole && pendingChange.status === (previousStatus === "true")) {
            modal.hide();
            return;
        }

        clearPageError();

        statusSelect.disabled = true;
        roleSelect.disabled = true;

        confirmBtn.disabled = true;
        confirmBtn.innerText = "Saving...";

        fetch(`/super_admin/api/user/${userId}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                ...csrfHeader()
            },
            body: JSON.stringify(pendingChange)
        })
            .then(async res => {
                if (!res.ok) {
                    const error = await res.json().catch(() => null);
                    throw new Error(error?.message || "Failed to save changes");
                }

                previousStatus = statusSelect.value;
                previousRole = roleSelect.value;

                showToast("Changes saved. The user will be asked to sign in again.");
            })
            .catch(error => {
                statusSelect.value = previousStatus;
                roleSelect.value = previousRole;

                showPageError(error.message || "Failed to save changes");
            })
            .finally(() => {
                statusSelect.disabled = false;
                roleSelect.disabled = false;

                confirmBtn.disabled = false;
                confirmBtn.innerText = "Confirm";

                modal.hide();
                pendingChange = null;
            });
    }

    statusSelect.addEventListener("change", () => openConfirm("status"));
    roleSelect.addEventListener("change", () => openConfirm("role"));

    confirmBtn.addEventListener("click", applyChange);

    confirmModalEl.addEventListener("hidden.bs.modal", () => {
        if (!pendingChange) return;

        statusSelect.value = previousStatus;
        roleSelect.value = previousRole;

        pendingChange = null;
    });
}

function showToast(message, isError = false) {
    const toast = document.createElement("div");

    toast.className = `alert ${isError ? "alert-danger" : "alert-success"} position-fixed top-0 end-0 m-3`;
    toast.style.zIndex = "9999";
    toast.innerText = message;

    document.body.appendChild(toast);

    setTimeout(() => toast.remove(), 2500);
}