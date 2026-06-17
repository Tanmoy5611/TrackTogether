const target = name => `[data-tutorial="${name}"]`;

function openProfilePreferences() {
    const preferences = document.querySelector(".profile-preferences");

    if (preferences) {
        preferences.open = true;
    }
}

export const tutorialSteps = [
    {
        id: "home-nav",
        route: "/",
        target: target("nav-home"),
        title: "Start from Home",
        body: "Home is your starting point after login. It brings together your next events, travel group options, CO2 progress, and quick links so you can decide what to do next.",
        icon: "bi-house-door",
        preferredPlacement: "bottom"
    },
    {
        id: "home-quick-actions",
        route: "/",
        target: target("home-quick-actions"),
        title: "Open events or groups quickly",
        body: "These two buttons take you straight to activity planning or travel groups. Use them when you already know whether you want to find an event or coordinate a trip.",
        icon: "bi-calendar2-plus",
        preferredPlacement: "left"
    },
    {
        id: "home-upcoming-events",
        route: "/",
        target: target("home-upcoming-events"),
        title: "Check your upcoming events",
        body: "This section shows your next planned activities, ordered by date, so the next event you should care about stays easy to find.",
        icon: "bi-calendar-event",
        preferredPlacement: "right"
    },
    {
        id: "home-travel-buddies",
        route: "/",
        target: target("home-travel-buddies"),
        title: "Find useful travel groups",
        body: "This area suggests travel groups that may match your route, timing, transport preferences, and available spots.",
        icon: "bi-people",
        preferredPlacement: "left"
    },
    {
        id: "home-suggested-activities",
        route: "/",
        target: target("home-suggested-activities"),
        title: "See matched activities",
        body: "Suggested activities highlight events that may fit your usual locations, timing, or what is coming up soon.",
        icon: "bi-stars",
        preferredPlacement: "right"
    },
    {
        id: "home-quick-insight",
        route: "/",
        target: target("home-quick-insight"),
        title: "Review quick insights",
        body: "Quick insights summarize what looks relevant today, such as location matches, time matches, and whether your profile is ready to use.",
        icon: "bi-lightbulb",
        preferredPlacement: "left"
    },
    {
        id: "activities-nav",
        route: "/activities",
        target: target("nav-activities"),
        title: "Go to Activities",
        body: "The Activities page is where you can browse events, join activities, and see what is available.",
        icon: "bi-calendar2-week",
        preferredPlacement: "bottom"
    },
    {
        id: "activities-create",
        route: "/activities",
        target: target("activities-create"),
        title: "Create a new activity",
        body: "Use this button to create a new activity that other regular users can discover and join.",
        icon: "bi-plus-circle",
        preferredPlacement: "top"
    },
    {
        id: "travelgroups-nav",
        route: "/travelgroups",
        target: target("nav-travelgroups"),
        title: "Open Travel Groups",
        body: "Travel Groups help users coordinate routes and transport so people can travel together more easily.",
        icon: "bi-signpost-2",
        preferredPlacement: "bottom"
    },
    {
        id: "travelgroups-create",
        route: "/travelgroups",
        target: target("travelgroups-create"),
        title: "Create or organize a group",
        body: "Use this action to start organizing a shared travel option or create the relevant item connected to group coordination.",
        icon: "bi-plus-circle",
        preferredPlacement: "left"
    },
    {
        id: "analytics-nav",
        route: "/analytics/me",
        target: target("nav-analytics"),
        title: "Track your impact",
        body: "Analytics shows your personal travel activity and impact, including useful statistics such as CO2 savings when available.",
        icon: "bi-graph-up-arrow",
        preferredPlacement: "bottom"
    },
    {
        id: "analytics-overview",
        route: "/analytics/me",
        target: target("analytics-overview"),
        title: "Understand your travel overview",
        body: "This dashboard helps you understand your own travel patterns and progress based on the activities and groups you use.",
        icon: "bi-bar-chart",
        preferredPlacement: "bottom"
    },
    {
        id: "chats-nav",
        route: "/chat",
        target: target("nav-chats"),
        title: "Open your chats",
        body: "Chats are where you can communicate with direct contacts and group members about activities or travel plans.",
        icon: "bi-chat-dots",
        preferredPlacement: "bottom"
    },
    {
        id: "profile-nav",
        route: "/member/profile",
        target: target("nav-profile"),
        title: "Manage your profile",
        body: "Your profile contains your account details and travel preferences. Keeping it updated helps the app give better activity and travel suggestions.",
        icon: "bi-person-circle",
        preferredPlacement: "bottom"
    },
    {
        id: "profile-preferences-summary",
        route: "/member/profile",
        target: target("profile-preferences-summary"),
        title: "Update travel preferences",
        body: "Open this section to adjust the preferences used for travel group matching and personalized suggestions.",
        icon: "bi-sliders",
        preferredPlacement: "left",
        beforeStep: openProfilePreferences
    },
    {
        id: "profile-transport-mode",
        route: "/member/profile",
        target: target("profile-transport-mode"),
        title: "Choose your preferred transport",
        body: "Select your usual transport mode so the system can better match you with relevant routes and travel groups.",
        icon: "bi-bus-front",
        preferredPlacement: "left",
        beforeStep: openProfilePreferences
    },
    {
        id: "profile-departure-location",
        route: "/member/profile",
        target: target("profile-departure-location"),
        title: "Set your default departure location",
        body: "Add your usual starting point so matching can work better for travel groups and route-based suggestions.",
        icon: "bi-geo-alt",
        preferredPlacement: "left",
        beforeStep: openProfilePreferences
    }
];