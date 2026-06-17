const LAST_ACTIVITY_KEY = "trackTogether.lastActivityAt";
const CHECK_INTERVAL_MS = 1000;
const KEEP_ALIVE_INTERVAL_MS = 5 * 60 * 1000;
const ACTIVITY_WRITE_THROTTLE_MS = 1000;
const LOGOUT_REDIRECT_URL = "/oauth2/authorization/google";

let fallbackLastActivityAt = Date.now();

document.addEventListener("DOMContentLoaded", () => {
    const container = document.querySelector("[data-inactivity-timeout-millis]");
    const timeoutMillis = Number(container?.dataset.inactivityTimeoutMillis);

    if (!Number.isFinite(timeoutMillis) || timeoutMillis <= 0) {
        return;
    }

    let logoutStarted = false;
    let lastActivityWriteAt = 0;
    let lastKeepAliveAt = Date.now();

    writeLastActivity(lastKeepAliveAt);

    const logoutAfterInactivity = () => {
        if (logoutStarted) {
            return;
        }

        logoutStarted = true;

        const logoutForm = document.querySelector("[data-inactivity-logout-form]");

        if (logoutForm) {
            HTMLFormElement.prototype.submit.call(logoutForm);
            return;
        }

        window.location.assign(LOGOUT_REDIRECT_URL);
    };

    const isInactive = now => now - readLastActivity() >= timeoutMillis;

    const handleActivity = () => {
        if (logoutStarted) {
            return;
        }

        const now = Date.now();

        if (isInactive(now)) {
            logoutAfterInactivity();
            return;
        }

        if (now - lastActivityWriteAt >= ACTIVITY_WRITE_THROTTLE_MS) {
            writeLastActivity(now);
            lastActivityWriteAt = now;
        }
    };

    const checkSession = () => {
        if (logoutStarted) {
            return;
        }

        const now = Date.now();

        if (isInactive(now)) {
            logoutAfterInactivity();
            return;
        }

        if (now - lastKeepAliveAt >= KEEP_ALIVE_INTERVAL_MS) {
            lastKeepAliveAt = now;
            keepSessionAlive().catch(() => {
                window.location.assign(LOGOUT_REDIRECT_URL);
            });
        }
    };

    [
        "click",
        "input",
        "keydown",
        "mousedown",
        "mousemove",
        "scroll",
        "touchstart",
        "wheel"
    ].forEach(eventName => {
        window.addEventListener(eventName, handleActivity, { passive: true });
    });

    document.addEventListener("visibilitychange", () => {
        if (!document.hidden) {
            checkSession();
        }
    });

    window.addEventListener("storage", event => {
        if (event.key === LAST_ACTIVITY_KEY) {
            checkSession();
        }
    });

    window.setInterval(checkSession, CHECK_INTERVAL_MS);
});

function readLastActivity() {
    let lastActivity = fallbackLastActivityAt;

    try {
        lastActivity = Number(window.localStorage.getItem(LAST_ACTIVITY_KEY));
    } catch {
        return fallbackLastActivityAt;
    }

    return Number.isFinite(lastActivity) && lastActivity > 0
        ? lastActivity
        : fallbackLastActivityAt;
}

function writeLastActivity(timestamp) {
    fallbackLastActivityAt = timestamp;

    try {
        window.localStorage.setItem(LAST_ACTIVITY_KEY, String(timestamp));
    } catch {
        // The in-memory value keeps the current tab protected if storage is unavailable.
    }
}

async function keepSessionAlive() {
    const response = await fetch("/session/keep-alive", {
        credentials: "same-origin",
        headers: {
            "X-Requested-With": "XMLHttpRequest"
        }
    });

    if (!response.ok || response.redirected) {
        throw new Error("Session is no longer active.");
    }
}