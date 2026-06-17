const chatSearchInput = document.getElementById("chatSearchInput");
const chatSearchItems = document.querySelectorAll(".chat-search-item");

chatSearchInput.addEventListener("input", function () {
    const searchValue = chatSearchInput.value.toLowerCase().trim();

    chatSearchItems.forEach(function (item) {
        const itemText = item.innerText.toLowerCase();

        if (itemText.includes(searchValue)) {
            item.style.display = "";
        } else {
            item.style.display = "none";
        }
    });
});

document.querySelectorAll("[data-chat-section-toggle]").forEach(function (toggle) {
    toggle.addEventListener("click", function () {
        const section = toggle.closest("[data-chat-section]");
        const body = section.querySelector("[data-chat-section-body]");
        if (isMobileChatHub()) {
            toggle.setAttribute("aria-expanded", "true");
            body.hidden = false;
            return;
        }
        const expanded = toggle.getAttribute("aria-expanded") === "true";

        toggle.setAttribute("aria-expanded", String(!expanded));
        body.hidden = expanded;
    });
});

setupMobileChatTabs();
syncMobileChatSections();
window.addEventListener("resize", syncMobileChatSections);

setupCreateGroupMemberSearch();
setupGroupInfoMemberPicker();
setupChatImagePicker();
setupChatImagePreview();
setupAutoSubmitFields();

const chatMessageList = document.getElementById("chatMessageList");
const chatMessageEmpty = document.getElementById("chatMessageEmpty");
const knownMessageIds = new Set(
    Array.from(chatMessageList?.querySelectorAll("[data-message-id]") || [])
        .map(message => message.dataset.messageId)
);
let chatSocket = null;
let chatSocketReconnectTimer = null;
let chatSocketManuallyClosed = false;
setupMobileChatViewport();

if (chatMessageList && chatMessageList.dataset.conversationId) {
    requestAnimationFrame(function () {
        chatMessageList.scrollTop = chatMessageList.scrollHeight;
    });
    connectChatSocket();
    window.addEventListener("beforeunload", closeChatSocket);
}

function connectChatSocket() {
    if (chatSocketManuallyClosed || chatSocket?.readyState === WebSocket.OPEN) {
        return;
    }

    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    const conversationId = chatMessageList.dataset.conversationId;
    chatSocket = new WebSocket(`${protocol}://${window.location.host}/ws/chat/${conversationId}`);

    chatSocket.addEventListener("message", function (event) {
        const message = JSON.parse(event.data);
        if (message.type === "seen") {
            markMessagesSeen(message.seenMessageIds);
            return;
        }

        if (!message.messageId || knownMessageIds.has(message.messageId)) {
            return;
        }

        const shouldScroll = isNearBottom(chatMessageList);
        knownMessageIds.add(message.messageId);
        chatMessageList.appendChild(renderLiveMessage(message));
        chatMessageList.hidden = false;
        if (chatMessageEmpty) {
            chatMessageEmpty.hidden = true;
        }
        if (shouldScroll) {
            chatMessageList.scrollTop = chatMessageList.scrollHeight;
        }
        if (message.senderId !== chatMessageList.dataset.currentMemberId) {
            sendSeenReceipt();
        }
    });

    chatSocket.addEventListener("open", function () {
        sendSeenReceipt();
    });

    chatSocket.addEventListener("close", function () {
        if (!chatSocketManuallyClosed) {
            window.clearTimeout(chatSocketReconnectTimer);
            chatSocketReconnectTimer = window.setTimeout(connectChatSocket, 5000);
        }
    });
}

function setupMobileChatViewport() {
    if (!document.body.classList.contains("chat-page--conversation")) {
        return;
    }

    const root = document.documentElement;
    const appHeader = document.querySelector("header");
    const messageInput = document.getElementById("chatMessageInput");
    const sendForm = document.querySelector(".chat-send-form");
    const viewport = window.visualViewport;

    function updateViewportSize() {
        const viewportHeight = viewport?.height || window.innerHeight;
        const headerHeight = appHeader?.getBoundingClientRect().height || 0;
        root.style.setProperty("--chat-visual-height", `${viewportHeight}px`);
        root.style.setProperty("--chat-site-header-height", `${headerHeight}px`);
        scrollMessagesToBottom();
    }

    function scrollMessagesToBottom() {
        if (!chatMessageList) {
            return;
        }

        window.requestAnimationFrame(function () {
            chatMessageList.scrollTop = chatMessageList.scrollHeight;
        });
    }

    updateViewportSize();
    window.addEventListener("resize", updateViewportSize);
    window.addEventListener("orientationchange", updateViewportSize);
    if (viewport) {
        viewport.addEventListener("resize", updateViewportSize);
        viewport.addEventListener("scroll", updateViewportSize);
    }
    messageInput?.addEventListener("focus", function () {
        window.setTimeout(updateViewportSize, 250);
    });
    sendForm?.addEventListener("submit", scrollMessagesToBottom);
}

function closeChatSocket() {
    chatSocketManuallyClosed = true;
    window.clearTimeout(chatSocketReconnectTimer);
    if (chatSocket) {
        chatSocket.close();
    }
}

function renderLiveMessage(message) {
    const currentMemberId = chatMessageList.dataset.currentMemberId;
    const selectedGroupChat = chatMessageList.dataset.selectedGroupChat === "true";
    const ownMessage = message.senderId === currentMemberId;

    const wrapper = document.createElement("div");
    wrapper.className = `chat-message ${ownMessage ? "chat-message--own" : "chat-message--other"}`;
    wrapper.dataset.messageId = message.messageId;

    const bubble = document.createElement("div");
    bubble.className = "chat-message-bubble";

    if (selectedGroupChat && !ownMessage) {
        const sender = document.createElement("strong");
        sender.className = "chat-message-sender";
        sender.textContent = firstName(message.senderName);
        bubble.appendChild(sender);
    }

    if (message.imageUrl) {
        const image = document.createElement("img");
        image.className = "chat-message-image";
        image.src = message.imageUrl;
        image.alt = message.imageFileName || "Image";
        image.dataset.chatImagePreview = "";
        bubble.appendChild(image);
    }

    const paragraph = document.createElement("p");
    if (message.message) {
        const text = document.createElement("span");
        text.textContent = message.message;
        paragraph.appendChild(text);
    }
    const time = document.createElement("span");
    time.className = "chat-message-time";
    time.textContent = formatChatTime(message.timestamp);
    paragraph.appendChild(time);

    if (ownMessage) {
        paragraph.appendChild(renderMessageStatus(message.seen ? "seen" : "sent"));
    }

    bubble.appendChild(paragraph);
    wrapper.appendChild(bubble);

    if (!ownMessage) {
        const reportButton = document.createElement("button");
        reportButton.type = "button";
        reportButton.className = "chat-message-report-btn";
        reportButton.dataset.messageId = message.messageId;
        reportButton.setAttribute("aria-label", "Report this message");
        reportButton.title = "Report this message";
        reportButton.innerHTML = '<i class="bi bi-flag" aria-hidden="true"></i>';
        wrapper.appendChild(reportButton);
    }

    return wrapper;
}

function renderMessageStatus(status) {
    const statusElement = document.createElement("span");
    statusElement.className = "chat-message-status";
    statusElement.dataset.messageStatus = status;
    statusElement.setAttribute("aria-label", status === "seen" ? "Seen" : "Sent");
    statusElement.title = status === "seen" ? "Seen" : "Sent";
    statusElement.innerHTML = [
        '<i class="bi bi-check-lg" aria-hidden="true"></i>',
        '<i class="bi bi-check-lg chat-message-status-seen" aria-hidden="true"></i>'
    ].join("");
    return statusElement;
}

function sendSeenReceipt() {
    if (!chatSocket || chatSocket.readyState !== WebSocket.OPEN) {
        return;
    }

    chatSocket.send(JSON.stringify({type: "seen"}));
}

function markMessagesSeen(messageIds) {
    if (!Array.isArray(messageIds)) {
        return;
    }

    messageIds.forEach(function (messageId) {
        const messageElement = Array.from(chatMessageList.querySelectorAll(".chat-message--own[data-message-id]"))
            .find(element => element.dataset.messageId === messageId);
        const statusElement = messageElement?.querySelector(".chat-message-status");
        if (!statusElement) {
            return;
        }

        statusElement.dataset.messageStatus = "seen";
        statusElement.setAttribute("aria-label", "Seen");
        statusElement.title = "Seen";
    });
}

function firstName(name) {
    return (name || "Sender").split(" ")[0];
}

function formatChatTime(timestamp) {
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) {
        return "";
    }
    return date.toLocaleTimeString([], {hour: "2-digit", minute: "2-digit"});
}

function isNearBottom(element) {
    return element.scrollHeight - element.scrollTop - element.clientHeight < 120;
}

function setupCreateGroupMemberSearch() {
    const input = document.getElementById("createGroupMemberSearch");
    const members = document.querySelectorAll(".chat-group-create-modal__member[data-member-search]");
    const empty = input?.closest(".chat-group-create-modal__members")
        ?.querySelector(".chat-member-search-empty");

    if (!input || members.length === 0) {
        return;
    }

    input.addEventListener("input", function () {
        const query = normalizeSearch(input.value);
        let visibleCount = 0;

        members.forEach(function (member) {
            const matches = normalizeSearch(member.dataset.memberSearch).includes(query);
            member.hidden = !matches;
            if (matches) {
                visibleCount += 1;
            }
        });

        if (empty) {
            empty.hidden = visibleCount > 0;
        }
    });
}

function setupGroupInfoMemberPicker() {
    document.querySelectorAll("[data-member-picker]").forEach(function (picker) {
        const searchInput = picker.querySelector("[data-member-picker-search]");
        const hiddenValue = picker.querySelector("[data-member-picker-value]");
        const options = Array.from(picker.querySelectorAll("[data-member-picker-option]"));
        const empty = picker.querySelector(".chat-member-search-empty");
        const submit = picker.closest("form")?.querySelector("[data-member-picker-submit]");

        if (!searchInput || !hiddenValue || options.length === 0) {
            return;
        }

        searchInput.addEventListener("input", function () {
            const query = normalizeSearch(searchInput.value);
            let visibleCount = 0;

            hiddenValue.value = "";
            if (submit) {
                submit.disabled = true;
            }
            options.forEach(option => option.removeAttribute("aria-selected"));

            options.forEach(function (option) {
                const matches = normalizeSearch(option.dataset.memberSearch).includes(query);
                option.hidden = !matches;
                if (matches) {
                    visibleCount += 1;
                }
            });

            if (empty) {
                empty.hidden = visibleCount > 0;
            }
        });

        options.forEach(function (option) {
            option.addEventListener("click", function () {
                hiddenValue.value = option.dataset.memberId;
                searchInput.value = option.dataset.memberName || option.textContent.trim();
                options.forEach(item => item.removeAttribute("aria-selected"));
                option.setAttribute("aria-selected", "true");
                if (submit) {
                    submit.disabled = false;
                }
            });
        });
    });
}

function setupChatImagePicker() {
    const imageInput = document.getElementById("chatImageInput");
    const selectedLabel = document.getElementById("chatImageSelected");
    const selectedPreview = document.getElementById("chatImageSelectedPreview");
    const selectedName = document.getElementById("chatImageSelectedName");
    const removeButton = document.getElementById("chatImageRemove");
    let selectedImageUrl = null;

    if (!imageInput || !selectedLabel || !selectedPreview || !selectedName || !removeButton) {
        return;
    }

    imageInput.addEventListener("change", function () {
        const file = imageInput.files?.[0];
        if (!file) {
            clearSelectedImage();
            return;
        }

        if (selectedImageUrl) {
            URL.revokeObjectURL(selectedImageUrl);
        }

        selectedImageUrl = URL.createObjectURL(file);
        selectedPreview.src = selectedImageUrl;
        selectedName.textContent = file.name;
        selectedLabel.hidden = false;
    });

    removeButton.addEventListener("click", clearSelectedImage);

    function clearSelectedImage() {
        imageInput.value = "";
        selectedLabel.hidden = true;
        selectedName.textContent = "";
        selectedPreview.removeAttribute("src");
        if (selectedImageUrl) {
            URL.revokeObjectURL(selectedImageUrl);
            selectedImageUrl = null;
        }
    }
}

function setupChatImagePreview() {
    const modal = document.getElementById("chatImagePreviewModal");
    const preview = document.getElementById("chatImagePreview");
    if (!modal || !preview) {
        return;
    }

    document.addEventListener("click", function (event) {
        const image = event.target.closest("[data-chat-image-preview]");
        if (image) {
            preview.src = image.currentSrc || image.src;
            preview.alt = image.alt || "Image";
            modal.hidden = false;
            document.body.classList.add("chat-image-preview-open");
            return;
        }

        if (event.target.closest("[data-chat-image-preview-close]")) {
            closeChatImagePreview(modal, preview);
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !modal.hidden) {
            closeChatImagePreview(modal, preview);
        }
    });
}

function setupAutoSubmitFields() {
    document.querySelectorAll("[data-auto-submit-on-change]").forEach(function (field) {
        field.addEventListener("change", function () {
            field.form?.submit();
        });
    });
}

function closeChatImagePreview(modal, preview) {
    modal.hidden = true;
    preview.removeAttribute("src");
    document.body.classList.remove("chat-image-preview-open");
}

function normalizeSearch(value) {
    return (value || "").toLowerCase().trim();
}

function isMobileChatHub() {
    return window.matchMedia("(max-width: 600px)").matches;
}

function syncMobileChatSections() {
    if (!isMobileChatHub()) {
        document.querySelectorAll("[data-chat-tab-panel]").forEach(function (panel) {
            panel.classList.add("is-active");
        });
        return;
    }

    document.querySelectorAll("[data-chat-section-toggle]").forEach(function (toggle) {
        const section = toggle.closest("[data-chat-section]");
        const body = section?.querySelector("[data-chat-section-body]");
        toggle.setAttribute("aria-expanded", "true");
        if (body) {
            body.hidden = false;
        }
    });
    activateMobileChatTab(getActiveMobileChatTab());
}

function setupMobileChatTabs() {
    document.querySelectorAll("[data-chat-tab]").forEach(function (tab) {
        tab.addEventListener("click", function () {
            activateMobileChatTab(tab.dataset.chatTab);
        });
    });
}

function getActiveMobileChatTab() {
    const activeTab = document.querySelector("[data-chat-tab].is-active");
    return activeTab?.dataset.chatTab || "direct";
}

function activateMobileChatTab(tabName) {
    const selectedTabName = tabName || "direct";
    document.querySelectorAll("[data-chat-tab]").forEach(function (tab) {
        const selected = tab.dataset.chatTab === selectedTabName;
        tab.classList.toggle("is-active", selected);
        tab.setAttribute("aria-selected", String(selected));
    });

    document.querySelectorAll("[data-chat-tab-panel]").forEach(function (panel) {
        const active = !isMobileChatHub() || panel.dataset.chatTabPanel === selectedTabName;
        panel.classList.toggle("is-active", active);
    });
}