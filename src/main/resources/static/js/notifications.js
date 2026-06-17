const bell       = document.getElementById("notifBell");
const badge      = document.getElementById("notifBadge");
const dropdown   = document.getElementById("notifDropdown");
const list       = document.getElementById("notifList");
const readAllBtn = document.getElementById("notifReadAll");
const labels     = bell?.dataset ?? {};

if (!bell) {
    throw new Error("Notification bell not found — skipping notifications.js");
}

function csrfHeaders() {
    const token  = bell.dataset.csrf;
    const header = bell.dataset.csrfHeader;
    return token && header ? { [header]: token } : {};
}

function label(name, fallback) {
    return labels[name] || fallback;
}

function formatTemplate(template, count) {
    return (template || "__count__").replace("__count__", count);
}

async function fetchUnreadCount() {
    try {
        const res = await fetch("/api/notifications/unread-count");
        if (!res.ok) return;
        const { count } = await res.json();
        badge.textContent = count > 99 ? "99+" : String(count);
        badge.hidden = count === 0;
        bell.classList.toggle("tt-notif-bell--has-unread", count > 0);
    } catch {
        // silently ignore network errors during polling
    }
}

async function fetchAndRenderNotifications() {
    try {
        const res = await fetch("/api/notifications");
        if (!res.ok) return;
        const notifications = await res.json();
        renderList(notifications);
    } catch {
        // silently ignore
    }
}

function renderList(notifications) {
    list.innerHTML = "";

    if (notifications.length === 0) {
        const empty = document.createElement("li");
        empty.className = "tt-notif-empty";
        empty.textContent = label("emptyLabel", "No notifications yet");
        list.appendChild(empty);
        return;
    }

    notifications.forEach(n => {
        const li = document.createElement("li");
        li.className = "tt-notif-item" + (n.read ? "" : " tt-notif-item--unread");
        li.dataset.id = n.id;

        const iconClass = n.type === "MEMBER_JOINED"          ? "bi-person-plus"
                        : n.type === "MEMBER_LEFT"            ? "bi-person-dash"
                        : n.type === "JOIN_REQUEST_RECEIVED"  ? "bi-person-plus-fill"
                        : n.type === "JOIN_REQUEST_ACCEPTED"  ? "bi-check-circle-fill"
                        : n.type === "JOIN_REQUEST_REJECTED"  ? "bi-x-circle"
                        : n.type === "NEW_MESSAGE"           ? "bi-chat-dots"
                        : "bi-people-fill";

        const icon = document.createElement("i");
        icon.className = "bi " + iconClass;
        icon.setAttribute("aria-hidden", "true");

        const text = document.createElement("span");
        text.className = "tt-notif-text";
        text.textContent = n.message;

        const time = document.createElement("time");
        time.className = "tt-notif-time";
        time.textContent = formatTime(n.createdAt);

        const inner = document.createElement("a");
        inner.href = `/travelgroups/${n.groupId}`;
        inner.className = "tt-notif-link";
        inner.appendChild(icon);
        inner.appendChild(text);

        // Mark as read on click, then navigate
        inner.addEventListener("click", e => {
            if (!n.read) {
                e.preventDefault();
                fetch(`/api/notifications/${n.id}/read`, {
                    method: "PUT",
                    headers: csrfHeaders(),
                    keepalive: true
                }).finally(() => {
                    n.read = true;
                    li.classList.remove("tt-notif-item--unread");
                    updateBadgeFromList();
                    window.location.href = inner.href;
                });
            }
        });

        li.appendChild(inner);
        li.appendChild(time);
        list.appendChild(li);
    });
}

function updateBadgeFromList() {
    const unreadCount = list.querySelectorAll(".tt-notif-item--unread").length;
    badge.textContent = unreadCount > 99 ? "99+" : String(unreadCount);
    badge.hidden = unreadCount === 0;
    bell.classList.toggle("tt-notif-bell--has-unread", unreadCount > 0);
}

function formatTime(isoString) {
    if (!isoString) return "";
    const date   = new Date(isoString);
    const now    = new Date();
    const diffMs  = now - date;
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1)  return label("justNowLabel", "just now");
    if (diffMin < 60) return formatTemplate(label("minutesAgoTemplate", "__count__m ago"), diffMin);
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24)   return formatTemplate(label("hoursAgoTemplate", "__count__h ago"), diffH);
    return date.toLocaleDateString();
}

async function markAllAsRead() {
    try {
        await fetch("/api/notifications/read-all", {
            method: "PUT",
            headers: csrfHeaders()
        });
        badge.textContent = "0";
        badge.hidden = true;
        bell.classList.remove("tt-notif-bell--has-unread");
        renderList([]);
    } catch {
        // silently ignore
    }
}

function toggleDropdown() {
    const isOpen = !dropdown.hidden;
    dropdown.hidden = isOpen;
    bell.setAttribute("aria-expanded", String(!isOpen));

    if (!isOpen) {
        fetchAndRenderNotifications();
    }
}

bell.addEventListener("click", e => {
    e.stopPropagation();
    toggleDropdown();
});

readAllBtn.addEventListener("click", e => {
    e.stopPropagation();
    markAllAsRead();
});

document.addEventListener("click", e => {
    if (!dropdown.hidden && !dropdown.contains(e.target) && e.target !== bell) {
        dropdown.hidden = true;
        bell.setAttribute("aria-expanded", "false");
    }
});

// Initial fetch + polling every 10 seconds
fetchUnreadCount();
setInterval(fetchUnreadCount, 10_000);