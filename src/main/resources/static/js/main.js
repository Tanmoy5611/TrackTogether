document.addEventListener("DOMContentLoaded", () => {
    initNavigation();
    showFlashToast();
});

function initNavigation() {
    const nav = document.querySelector(".kdg-nav");

    if (!nav || nav.dataset.initialized === "true") {
        return;
    }

    nav.dataset.initialized = "true";

    const navItemLinks = nav.querySelectorAll("[data-nav-item]");
    const availableItems = new Set([...navItemLinks].map(link => link.dataset.navItem));
    const path = normalizePath(window.location.pathname);
    const active = activeNavItem(path, availableItems);

    navItemLinks.forEach(link => {
        const isActive = link.dataset.navItem === active;

        link.classList.toggle("kdg-nav__link--active", isActive && link.classList.contains("kdg-nav__link"));
        link.classList.toggle("kdg-enrol--active", isActive && link.classList.contains("kdg-enrol"));

        if (isActive) {
            link.setAttribute("aria-current", "page");
        } else {
            link.removeAttribute("aria-current");
        }
    });

    const navToggle = nav.querySelector(".kdg-nav__toggle");
    const navLinks = nav.querySelector(".kdg-nav__links");

    if (navToggle && navLinks) {
        navToggle.addEventListener("click", () => {
            const isOpen = navLinks.classList.toggle("is-open");
            navToggle.classList.toggle("is-open", isOpen);
            navToggle.setAttribute("aria-expanded", String(isOpen));
        });

        navLinks.querySelectorAll("a").forEach(link => {
            link.addEventListener("click", () => {
                navLinks.classList.remove("is-open");
                navToggle.classList.remove("is-open");
                navToggle.setAttribute("aria-expanded", "false");
            });
        });
    }
}

function showFlashToast() {
    const flashToast = document.getElementById("flashToast");
    if (flashToast && window.bootstrap) {
        bootstrap.Toast.getOrCreateInstance(flashToast).show();
    }
}

function normalizePath(pathname) {
    const path = pathname.toLowerCase().replace(/\/+$/, "");
    return path || "/";
}

function activeNavItem(path, availableItems) {
    if (matchesAny(path, [
        "/super_admin/activities",
        "/allactivities.html"
    ])) {
        if (availableItems.has("dashboard")) {
            return "dashboard";
        }

        if (availableItems.has("moderation")) {
            return "moderation";
        }

        return "activities";
    }

    if (matchesAny(path, [
        "/admin/dashboard",
        "/super_admin/home",
        "/super_admin/user",
        "/super_admin/settings",
        "/superadmindashboard.html",
        "/allusers.html",
        "/system-settings.html",
        "/usermanagement.html"
    ])) {
        return "dashboard";
    }

    if (matchesAny(path, [
        "/analytics",
        "/useranalytics.html",
        "/analyticsdashboard.html"
    ])) {
        return "analytics";
    }

    if (matchesAny(path, [
        "/moderator",
        "/moderator.html"
    ])) {
        return "moderation";
    }

    if (matchesAny(path, [
        "/activities",
        "/activities.html",
        "/activity-overview.html",
        "/newactivity.html"
    ])) {
        return "activities";
    }

    if (matchesAny(path, [
        "/travelgroups",
        "/travelgroups.html",
        "/travelgroup-detail.html",
        "/create-travelgroup.html"
    ])) {
        return "travelgroups";
    }

    if (matchesAny(path, [
        "/chat",
        "/chat-overview.html",
        "/contacts.html"
    ])) {
        return "chats";
    }

    if (matchesAny(path, [
        "/member/profile",
        "/useroverview.html"
    ])) {
        return "profile";
    }

    return "home";
}

function matchesAny(path, prefixes) {
    const file = `/${path.substring(path.lastIndexOf("/") + 1)}`;

    return prefixes.some(prefix =>
        path === prefix ||
        path.startsWith(`${prefix}/`) ||
        file === prefix
    );
}