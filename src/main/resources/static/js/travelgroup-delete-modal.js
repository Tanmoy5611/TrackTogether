document.addEventListener("DOMContentLoaded", () => {
    const deleteForm = document.getElementById("deleteTravelGroupForm");
    const redirectInput = document.getElementById("deleteTravelGroupRedirect");

    if (!deleteForm || !redirectInput) {
        return;
    }

    document.querySelectorAll(".js-travelgroup-delete-button").forEach((button) => {
        button.addEventListener("click", () => {
            // The same Bootstrap modal is reused by every card, so the clicked button fills in the form
            deleteForm.action = button.dataset.deleteUrl;
            redirectInput.value = button.dataset.redirectTo || redirectInput.value;
        });
    });
});