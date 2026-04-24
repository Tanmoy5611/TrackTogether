document.addEventListener("DOMContentLoaded", () => {

    const toastElement = document.getElementById("actionToast");
    const toastMessage = document.getElementById("toastMessage");
    const csrfToken = document.querySelector("meta[name='_csrf']")?.content;
    const csrfHeader = document.querySelector("meta[name='_csrf_header']")?.content;

    const toast = window.bootstrap && toastElement
        ? new bootstrap.Toast(toastElement)
        : null;

    function csrfHeaders() {
        return csrfToken && csrfHeader
            ? {[csrfHeader]: csrfToken}
            : {};
    }

    function showToast(message) {
        if (toast && toastMessage) {
            toastMessage.textContent = message;
            toast.show();
            return;
        }

        window.alert(message);
    }

    function setLoading(button, loading) {

        const text = button.querySelector(".btn-text");
        const spinner = button.querySelector(".spinner-border");

        if (loading) {
            button.disabled = true;
            text.classList.add("d-none");
            spinner.classList.remove("d-none");
        } else {
            button.disabled = false;
            text.classList.remove("d-none");
            spinner.classList.add("d-none");
        }
    }

    // JOIN
    document.querySelectorAll(".join-btn").forEach(button => {

        button.addEventListener("click", async () => {

            const groupId = button.dataset.groupId;

            if (!groupId) {
                showToast("Open this page through Spring Boot to join a travel group.");
                return;
            }

            setLoading(button, true);

            try {

                const response = await fetch(
                    `/api/travelgroups/${groupId}/join`,
                    {
                        method: "POST",
                        headers: csrfHeaders()
                    }
                );

                if (!response.ok) {
                    throw new Error("Could not join group");
                }

                showToast("Joined successfully");

            } catch (error) {

                showToast(error.message);

            } finally {

                setLoading(button, false);

            }

        });

    });


    // LEAVE
    document.querySelectorAll(".leave-btn").forEach(button => {

        button.addEventListener("click", async () => {

            const groupId = button.dataset.groupId;

            if (!groupId) {
                showToast("Open this page through Spring Boot to leave a travel group.");
                return;
            }

            setLoading(button, true);

            try {

                const response = await fetch(
                    `/api/travelgroups/${groupId}/leave`,
                    {
                        method: "DELETE",
                        headers: csrfHeaders()
                    }
                );

                if (!response.ok) {
                    throw new Error("Could not leave group");
                }

                // Remove card from UI
                const card = document.getElementById(`group-card-${groupId}`);
                if (card) {
                    card.remove();
                }

                showToast("Left travel group");

            } catch (error) {

                showToast(error.message);

            } finally {

                setLoading(button, false);

            }

        });

    });

});