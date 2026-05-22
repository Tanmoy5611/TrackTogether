(function () {
    const page = document.querySelector(".route-planner-page");

    if (!page) {
        return;
    }

    const shell = page.closest(".kdg-shell");
    const footer = shell ? shell.querySelector(".tt-footer") : document.querySelector(".tt-footer");
    const header = shell ? shell.querySelector(".kdg-nav") : document.querySelector(".kdg-nav");

    if (!footer) {
        return;
    }

    // Prevents old horizontal scroll positions from keeping this page shifted sideways
    const resetHorizontalScroll = () => {
        window.scrollTo(0, window.scrollY);
        document.documentElement.scrollLeft = 0;
        document.body.scrollLeft = 0;
    };

    // Sets the main route page height so the footer rests at the viewport bottom when content is short
    const stickFooter = () => {
        resetHorizontalScroll();

        const viewportHeight = window.visualViewport ? window.visualViewport.height : window.innerHeight;
        const headerHeight = header ? header.getBoundingClientRect().height : 0;
        const footerHeight = footer.getBoundingClientRect().height;
        const pageStyles = window.getComputedStyle(page);
        const verticalMargins = parseFloat(pageStyles.marginTop) + parseFloat(pageStyles.marginBottom);
        const availableHeight = Math.max(0, viewportHeight - headerHeight - footerHeight - verticalMargins);

        page.style.minHeight = `${availableHeight}px`;
    };

    // Defers footer recalculation until the browser has applied the latest layout change
    const scheduleStickFooter = () => {
        window.requestAnimationFrame(stickFooter);
    };

    stickFooter();
    window.addEventListener("load", stickFooter);
    window.addEventListener("resize", scheduleStickFooter);
    window.addEventListener("scroll", resetHorizontalScroll, {passive: true});

    if (window.visualViewport) {
        window.visualViewport.addEventListener("resize", scheduleStickFooter);
    }

    if ("ResizeObserver" in window) {
        // Recalculate when route content, footer, or responsive nav height changes
        const observer = new ResizeObserver(scheduleStickFooter);
        observer.observe(page);
        observer.observe(footer);
        if (header) {
            observer.observe(header);
        }
    }
})();