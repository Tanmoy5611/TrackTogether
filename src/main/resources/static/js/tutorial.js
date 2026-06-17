import { tutorialSteps } from "./tutorial-steps.js";

const STORAGE_KEY = "trackTogetherTutorialStep";
const STORAGE_MAX_AGE_MS = 30000;
const GAP = 20;
const VIEWPORT_MARGIN = 16;
const MIN_CARD_SPACE = 96;
const PLACEMENTS = ["right", "left", "bottom", "top"];
const FOCUSABLE_SELECTOR = [
    "button:not([disabled])",
    "[href]",
    "input:not([disabled])",
    "select:not([disabled])",
    "textarea:not([disabled])",
    "[tabindex]:not([tabindex='-1'])"
].join(",");

let activeTutorial;

export function initTutorial() {
    const startButton = document.querySelector("[data-tutorial-start]");

    if (!startButton) {
        clearStoredStep();
        return;
    }

    if (startButton.dataset.initialized === "true") {
        return;
    }

    startButton.dataset.initialized = "true";
    startButton.addEventListener("click", () => {
        activeTutorial?.close();
        activeTutorial = new TutorialController(tutorialSteps);
        activeTutorial.start(0);
    });

    const storedStep = readStoredStep();

    if (storedStep !== null) {
        activeTutorial = new TutorialController(tutorialSteps);
        activeTutorial.start(storedStep);
    }
}

class TutorialController {
    constructor(steps) {
        this.steps = steps;
        this.currentIndex = 0;
        this.currentTarget = null;
        this.navigationDirection = 1;
        this.previousFocus = document.activeElement;
        this.updateFrame = null;
        this.boundUpdate = () => this.schedulePositionUpdate();
        this.boundKeydown = event => this.handleKeydown(event);
        this.boundScrollBlock = event => this.blockManualScroll(event);
    }

    async start(index) {
        this.currentIndex = clampIndex(index, this.steps.length);
        saveStoredStep(this.currentIndex);

        if (!this.isCurrentRoute()) {
            this.navigateToCurrentRoute();
            return;
        }

        this.createElements();
        this.bindEvents();
        await this.showCurrentStep(this.navigationDirection);
    }

    async goTo(index, direction = index >= this.currentIndex ? 1 : -1) {
        this.currentIndex = clampIndex(index, this.steps.length);
        this.navigationDirection = direction;
        saveStoredStep(this.currentIndex);

        if (!this.isCurrentRoute()) {
            this.navigateToCurrentRoute();
            return;
        }

        await this.showCurrentStep(direction);
    }

    next() {
        if (this.currentIndex >= this.steps.length - 1) {
            this.close();
            return;
        }

        this.goTo(this.currentIndex + 1, 1);
    }

    previous() {
        this.goTo(this.currentIndex - 1, -1);
    }

    close() {
        clearStoredStep();
        this.unbindEvents();
        document.body.classList.remove("tutorial-active");
        this.overlay?.remove();
        this.highlight?.remove();
        this.card?.remove();

        if (this.previousFocus && typeof this.previousFocus.focus === "function") {
            this.previousFocus.focus({ preventScroll: true });
        }

        activeTutorial = null;
    }

    createElements() {
        if (this.card) {
            return;
        }

        this.overlay = document.createElement("div");
        this.overlay.className = "tutorial-overlay";
        this.overlay.setAttribute("aria-hidden", "true");

        this.highlight = document.createElement("div");
        this.highlight.className = "tutorial-highlight";
        this.highlight.setAttribute("aria-hidden", "true");

        this.card = document.createElement("section");
        this.card.className = "tutorial-card";
        this.card.setAttribute("role", "dialog");
        this.card.setAttribute("aria-modal", "true");
        this.card.setAttribute("aria-labelledby", "tutorial-title");
        this.card.setAttribute("aria-describedby", "tutorial-body");
        this.card.tabIndex = -1;

        this.card.innerHTML = `
            <button class="tutorial-card__close" type="button" data-tutorial-close aria-label="Close tutorial">
                <i class="bi bi-x-lg" aria-hidden="true"></i>
            </button>
            <p class="tutorial-card__step" data-tutorial-counter></p>
            <h2 class="tutorial-card__title" id="tutorial-title" data-tutorial-title></h2>
            <div class="tutorial-card__icon">
                <i class="bi" data-tutorial-icon aria-hidden="true"></i>
            </div>
            <p class="tutorial-card__body" id="tutorial-body" data-tutorial-body></p>
            <div class="tutorial-card__actions">
                <button class="kdg-button kdg-button--secondary tutorial-card__button" type="button" data-tutorial-previous>
                    <i class="bi bi-arrow-left" aria-hidden="true"></i>
                    <span>Previous</span>
                </button>
                <button class="kdg-button tutorial-card__button" type="button" data-tutorial-next>
                    <span>Next</span>
                    <i class="bi bi-arrow-right" aria-hidden="true"></i>
                </button>
            </div>
            <button class="tutorial-card__skip" type="button" data-tutorial-skip>Skip tutorial</button>
        `;

        this.card.querySelector("[data-tutorial-close]").addEventListener("click", () => this.close());
        this.card.querySelector("[data-tutorial-skip]").addEventListener("click", () => this.close());
        this.card.querySelector("[data-tutorial-next]").addEventListener("click", () => this.next());
        this.card.querySelector("[data-tutorial-previous]").addEventListener("click", () => this.previous());

        document.body.append(this.overlay, this.highlight, this.card);
        document.body.classList.add("tutorial-active");
    }

    bindEvents() {
        window.addEventListener("resize", this.boundUpdate);
        window.addEventListener("scroll", this.boundUpdate, { passive: true });
        window.addEventListener("keydown", this.boundKeydown, true);
        window.addEventListener("wheel", this.boundScrollBlock, { passive: false, capture: true });
        window.addEventListener("touchmove", this.boundScrollBlock, { passive: false, capture: true });
    }

    unbindEvents() {
        window.removeEventListener("resize", this.boundUpdate);
        window.removeEventListener("scroll", this.boundUpdate);
        window.removeEventListener("keydown", this.boundKeydown, true);
        window.removeEventListener("wheel", this.boundScrollBlock, { capture: true });
        window.removeEventListener("touchmove", this.boundScrollBlock, { capture: true });

        if (this.updateFrame) {
            cancelAnimationFrame(this.updateFrame);
        }
    }

    async showCurrentStep(direction = 1) {
        const step = this.currentStep();

        step.beforeStep?.();

        const target = await waitForTarget(step.target);

        if (!target) {
            this.skipUnavailableStep(direction);
            return;
        }

        this.currentTarget = target;
        this.renderStep(step);
        openMobileNavigationIfNeeded(target);
        await this.scrollTargetIntoView(target);
        this.updatePosition();
        await nextFrame();
        this.updatePosition();
        this.focusCard();
    }

    skipUnavailableStep(direction) {
        const nextIndex = this.currentIndex + (direction >= 0 ? 1 : -1);

        if (nextIndex < 0 || nextIndex >= this.steps.length) {
            this.close();
            return;
        }

        this.goTo(nextIndex, direction);
    }

    renderStep(step) {
        this.card.querySelector("[data-tutorial-counter]").textContent = `Step ${this.currentIndex + 1} of ${this.steps.length}`;
        this.card.querySelector("[data-tutorial-title]").textContent = step.title;
        this.card.querySelector("[data-tutorial-body]").textContent = step.body;
        this.card.querySelector("[data-tutorial-icon]").className = `bi ${step.icon || "bi-info-circle"}`;

        const previous = this.card.querySelector("[data-tutorial-previous]");
        const next = this.card.querySelector("[data-tutorial-next] span");

        previous.disabled = this.currentIndex === 0;
        next.textContent = this.currentIndex === this.steps.length - 1 ? "Finish" : "Next";
    }

    async scrollTargetIntoView(target) {
        const rect = target.getBoundingClientRect();
        const targetTop = rect.top + window.scrollY;
        const targetCenter = targetTop + rect.height / 2;
        const desiredTop = Math.max(0, targetCenter - window.innerHeight / 2);

        window.scrollTo({
            top: desiredTop,
            behavior: "smooth"
        });

        this.updatePosition();
        await waitForScrollSettle();
    }

    schedulePositionUpdate() {
        if (this.updateFrame) {
            return;
        }

        this.updateFrame = requestAnimationFrame(() => {
            this.updateFrame = null;
            this.updatePosition();
        });
    }

    updatePosition() {
        if (!this.currentTarget || !this.card || !this.highlight) {
            return;
        }

        const targetRect = this.currentTarget.getBoundingClientRect();

        const highlightPadding = 10;

        this.highlight.style.transform = `translate(${targetRect.left - highlightPadding}px, ${targetRect.top - highlightPadding}px)`;
        this.highlight.style.width = `${targetRect.width + highlightPadding * 2}px`;
        this.highlight.style.height = `${targetRect.height + highlightPadding * 2}px`;

        const placement = choosePlacement(
            targetRect,
            this.card,
            this.currentStep().preferredPlacement
        );

        this.card.dataset.placement = placement.name;
        this.card.style.left = `${placement.left}px`;
        this.card.style.top = `${placement.top}px`;
        this.card.style.maxHeight = `${placement.maxHeight}px`;
    }

    focusCard() {
        const preferredFocus = this.card.querySelector("[data-tutorial-next]");
        (preferredFocus || this.card).focus({ preventScroll: true });
    }

    handleKeydown(event) {
        if (event.key === "Escape") {
            event.preventDefault();
            this.close();
            return;
        }

        if (event.key === "ArrowRight") {
            event.preventDefault();
            this.next();
            return;
        }

        if (event.key === "ArrowLeft") {
            event.preventDefault();
            this.previous();
            return;
        }

        if (event.key === "Tab") {
            this.keepFocusInsideCard(event);
            return;
        }

        if (["PageDown", "PageUp", "Home", "End", " "].includes(event.key) && !this.card.contains(event.target)) {
            event.preventDefault();
        }
    }

    keepFocusInsideCard(event) {
        const focusable = [...this.card.querySelectorAll(FOCUSABLE_SELECTOR)]
            .filter(element => element.offsetParent !== null);

        if (focusable.length === 0) {
            event.preventDefault();
            this.card.focus({ preventScroll: true });
            return;
        }

        const first = focusable[0];
        const last = focusable[focusable.length - 1];

        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus({ preventScroll: true });
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus({ preventScroll: true });
        }
    }

    blockManualScroll(event) {
        if (this.card?.contains(event.target)) {
            return;
        }

        event.preventDefault();
    }

    currentStep() {
        return this.steps[this.currentIndex];
    }

    isCurrentRoute() {
        return normalizePath(window.location.pathname) === normalizePath(this.currentStep().route);
    }

    navigateToCurrentRoute() {
        window.location.assign(this.currentStep().route);
    }
}

function choosePlacement(targetRect, card, preferredPlacement) {
    card.style.maxHeight = `${window.innerHeight - VIEWPORT_MARGIN * 2}px`;

    const cardRect = card.getBoundingClientRect();
    const order = preferredPlacement
        ? [preferredPlacement, ...PLACEMENTS.filter(placement => placement !== preferredPlacement)]
        : PLACEMENTS;

    for (const placement of order) {
        const candidate = candidateForPlacement(placement, targetRect, cardRect);

        if (candidate && fitsViewport(candidate) && doesNotOverlapTarget(candidate, targetRect)) {
            return candidate;
        }
    }

    const flexibleCandidate = flexibleFallback(targetRect, cardRect, order);

    if (flexibleCandidate) {
        return flexibleCandidate;
    }

    const emergencyCandidate = emergencyEdgeFallback(targetRect, cardRect);

    if (emergencyCandidate) {
        return emergencyCandidate;
    }

    return {
        name: "center",
        left: Math.round((window.innerWidth - cardRect.width) / 2),
        top: Math.round((window.innerHeight - cardRect.height) / 2),
        right: Math.round((window.innerWidth + cardRect.width) / 2),
        bottom: Math.round((window.innerHeight + cardRect.height) / 2),
        maxHeight: window.innerHeight - VIEWPORT_MARGIN * 2
    };
}

function candidateForPlacement(name, targetRect, cardRect) {
    const width = cardRect.width;
    const height = cardRect.height;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    if (name === "right") {
        return buildCandidate(name, targetRect.right + GAP, clamp(
            targetRect.top + targetRect.height / 2 - height / 2,
            VIEWPORT_MARGIN,
            viewportHeight - height - VIEWPORT_MARGIN
        ), width, height);
    }

    if (name === "left") {
        return buildCandidate(name, targetRect.left - width - GAP, clamp(
            targetRect.top + targetRect.height / 2 - height / 2,
            VIEWPORT_MARGIN,
            viewportHeight - height - VIEWPORT_MARGIN
        ), width, height);
    }

    if (name === "bottom") {
        return buildCandidate(name, clamp(
            targetRect.left + targetRect.width / 2 - width / 2,
            VIEWPORT_MARGIN,
            viewportWidth - width - VIEWPORT_MARGIN
        ), targetRect.bottom + GAP, width, height);
    }

    if (name === "top") {
        return buildCandidate(name, clamp(
            targetRect.left + targetRect.width / 2 - width / 2,
            VIEWPORT_MARGIN,
            viewportWidth - width - VIEWPORT_MARGIN
        ), targetRect.top - height - GAP, width, height);
    }

    return null;
}

function flexibleFallback(targetRect, cardRect, order) {
    const viewportHeight = window.innerHeight;
    const bottomSpace = viewportHeight - targetRect.bottom - GAP - VIEWPORT_MARGIN;
    const topSpace = targetRect.top - GAP - VIEWPORT_MARGIN;
    const verticalCandidates = [
        { placement: "bottom", space: bottomSpace },
        { placement: "top", space: topSpace }
    ].sort((a, b) => {
        const orderDelta = order.indexOf(a.placement) - order.indexOf(b.placement);
        return b.space - a.space || orderDelta;
    });

    for (const candidate of verticalCandidates) {
        if (candidate.space < MIN_CARD_SPACE) {
            continue;
        }

        const height = Math.min(cardRect.height, candidate.space);
        const top = candidate.placement === "bottom"
            ? targetRect.bottom + GAP
            : targetRect.top - GAP - height;
        const left = clamp(
            targetRect.left + targetRect.width / 2 - cardRect.width / 2,
            VIEWPORT_MARGIN,
            window.innerWidth - cardRect.width - VIEWPORT_MARGIN
        );
        const flexible = buildCandidate(candidate.placement, left, top, cardRect.width, height);

        if (fitsViewport(flexible) && doesNotOverlapTarget(flexible, targetRect)) {
            return flexible;
        }
    }

    return null;
}

function emergencyEdgeFallback(targetRect, cardRect) {
    const spaces = [
        {
            placement: "bottom",
            space: window.innerHeight - targetRect.bottom - GAP - VIEWPORT_MARGIN
        },
        {
            placement: "top",
            space: targetRect.top - GAP - VIEWPORT_MARGIN
        },
        {
            placement: "right",
            space: window.innerWidth - targetRect.right - GAP - VIEWPORT_MARGIN
        },
        {
            placement: "left",
            space: targetRect.left - GAP - VIEWPORT_MARGIN
        }
    ].filter(item => item.space > 0)
        .sort((a, b) => b.space - a.space);

    for (const item of spaces) {
        const candidate = emergencyCandidateForPlacement(item.placement, targetRect, cardRect, item.space);

        if (candidate && fitsViewport(candidate) && doesNotOverlapTarget(candidate, targetRect)) {
            return candidate;
        }
    }

    return null;
}

function emergencyCandidateForPlacement(name, targetRect, cardRect, space) {
    const width = Math.min(cardRect.width, window.innerWidth - VIEWPORT_MARGIN * 2);
    const height = Math.min(cardRect.height, window.innerHeight - VIEWPORT_MARGIN * 2);

    if (name === "bottom") {
        return buildCandidate(name, clamp(
            targetRect.left + targetRect.width / 2 - width / 2,
            VIEWPORT_MARGIN,
            window.innerWidth - width - VIEWPORT_MARGIN
        ), targetRect.bottom + GAP, width, Math.max(1, Math.min(height, space)));
    }

    if (name === "top") {
        const candidateHeight = Math.max(1, Math.min(height, space));

        return buildCandidate(name, clamp(
            targetRect.left + targetRect.width / 2 - width / 2,
            VIEWPORT_MARGIN,
            window.innerWidth - width - VIEWPORT_MARGIN
        ), targetRect.top - GAP - candidateHeight, width, candidateHeight);
    }

    if (name === "right") {
        if (space < width) {
            return null;
        }

        return buildCandidate(name, targetRect.right + GAP, clamp(
            targetRect.top + targetRect.height / 2 - height / 2,
            VIEWPORT_MARGIN,
            window.innerHeight - height - VIEWPORT_MARGIN
        ), width, height);
    }

    if (name === "left") {
        if (space < width) {
            return null;
        }

        return buildCandidate(name, targetRect.left - GAP - width, clamp(
            targetRect.top + targetRect.height / 2 - height / 2,
            VIEWPORT_MARGIN,
            window.innerHeight - height - VIEWPORT_MARGIN
        ), width, height);
    }

    return null;
}

function buildCandidate(name, left, top, width, height) {
    const roundedLeft = Math.round(left);
    const roundedTop = Math.round(top);
    const roundedWidth = Math.round(width);
    const roundedHeight = Math.round(height);

    return {
        name,
        left: roundedLeft,
        top: roundedTop,
        right: roundedLeft + roundedWidth,
        bottom: roundedTop + roundedHeight,
        maxHeight: roundedHeight
    };
}

function fitsViewport(candidate) {
    return candidate.left >= VIEWPORT_MARGIN
        && candidate.top >= VIEWPORT_MARGIN
        && candidate.right <= window.innerWidth - VIEWPORT_MARGIN
        && candidate.bottom <= window.innerHeight - VIEWPORT_MARGIN;
}

function doesNotOverlapTarget(candidate, targetRect) {
    const expandedTarget = {
        left: targetRect.left - GAP,
        right: targetRect.right + GAP,
        top: targetRect.top - GAP,
        bottom: targetRect.bottom + GAP
    };

    return candidate.right <= expandedTarget.left
        || candidate.left >= expandedTarget.right
        || candidate.bottom <= expandedTarget.top
        || candidate.top >= expandedTarget.bottom;
}

function openMobileNavigationIfNeeded(target) {
    const navLinks = target.closest(".kdg-nav__links");
    const nav = target.closest(".kdg-nav");
    const navToggle = nav?.querySelector(".kdg-nav__toggle");

    if (!navLinks || !navToggle || navLinks.classList.contains("is-open")) {
        return;
    }

    const rect = target.getBoundingClientRect();

    if (rect.width === 0 || rect.height === 0) {
        navLinks.classList.add("is-open");
        navToggle.classList.add("is-open");
        navToggle.setAttribute("aria-expanded", "true");
    }
}

function waitForTarget(selector) {
    const startedAt = performance.now();

    return new Promise(resolve => {
        const findTarget = () => {
            const element = document.querySelector(selector);

            if (element) {
                resolve(element);
                return;
            }

            if (performance.now() - startedAt > 4000) {
                resolve(null);
                return;
            }

            requestAnimationFrame(findTarget);
        };

        findTarget();
    });
}

function waitForScrollSettle() {
    return new Promise(resolve => {
        let lastY = window.scrollY;
        let stableFrames = 0;
        let frames = 0;

        const check = () => {
            frames += 1;
            const currentY = window.scrollY;

            if (Math.abs(currentY - lastY) < 1) {
                stableFrames += 1;
            } else {
                stableFrames = 0;
            }

            lastY = currentY;

            if (stableFrames >= 8 || frames >= 90) {
                resolve();
                return;
            }

            requestAnimationFrame(check);
        };

        requestAnimationFrame(check);
    });
}

function nextFrame() {
    return new Promise(resolve => requestAnimationFrame(() => resolve()));
}

function normalizePath(path) {
    const normalized = path.toLowerCase().replace(/\/+$/, "");
    return normalized || "/";
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
}

function clampIndex(index, length) {
    return clamp(Number(index) || 0, 0, length - 1);
}

function readStoredStep() {
    try {
        const value = JSON.parse(sessionStorage.getItem(STORAGE_KEY));

        if (value === null) {
            return null;
        }

        if (typeof value !== "object" || value === null || !Object.hasOwn(value, "step") || !Object.hasOwn(value, "savedAt")) {
            clearStoredStep();
            return null;
        }

        if (Date.now() - Number(value.savedAt) > STORAGE_MAX_AGE_MS) {
            clearStoredStep();
            return null;
        }

        const step = Number(value.step);
        return Number.isInteger(step) && step >= 0 && step < tutorialSteps.length ? step : null;
    } catch {
        clearStoredStep();
        return null;
    }
}

function saveStoredStep(index) {
    try {
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
            step: index,
            savedAt: Date.now()
        }));
    } catch {
        // Session storage can be unavailable in restricted browsing modes.
    }
}

function clearStoredStep() {
    try {
        sessionStorage.removeItem(STORAGE_KEY);
    } catch {
        // Session storage can be unavailable in restricted browsing modes.
    }
}