// for showing active page in navbar
document.addEventListener("DOMContentLoaded", () => {

    const path = window.location.pathname.toLowerCase();
    const file = path.substring(path.lastIndexOf("/") + 1);

    let active = "home";

    if (
        path.includes("travelgroups") ||
        file === "travelgroups.html" ||
        file === "create-travelgroup.html"
    ) {
        active = "travelgroups";

    } else if (
        path.includes("chat") ||
        file === "chat-overview.html"
    ) {
        active = "chats";

    } else if (
        path.includes("moderator") ||
        file === "moderator.html"
    ) {
        active = "moderation";

    } else if (
        path.includes("profile") ||
        path.includes("member") ||
        file === "useroverview.html"
    ) {
        active = "profile";
    }

    document.querySelectorAll("[data-nav-item]").forEach(link => {

        const isActive = link.dataset.navItem === active;

        if (link.classList.contains("kdg-nav__link")) {
            link.classList.toggle("kdg-nav__link--active", isActive);
        }

        if (link.classList.contains("kdg-enrol")) {
            link.classList.toggle("kdg-enrol--active", isActive);
        }

    });

});