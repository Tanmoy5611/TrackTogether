package TrackTogether.controller;

import TrackTogether.domain.Conversation;
import TrackTogether.domain.ConversationType;
import TrackTogether.domain.Member;
import TrackTogether.domain.MemberConversation;
import TrackTogether.domain.MemberConversationRole;
import TrackTogether.domain.Message;
import TrackTogether.service.ConversationService;
import TrackTogether.service.CurrentUserService;
import TrackTogether.service.MemberService;
import TrackTogether.service.MessageService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final MessageService messageService;
    private final ConversationService conversationService;
    private final MemberService memberService;
    private final CurrentUserService currentUserService;
    private final MessageSource messageSource;

    public ChatController(MessageService messageService,
                          ConversationService conversationService,
                          MemberService memberService,
                          CurrentUserService currentUserService,
                          MessageSource messageSource) {
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.memberService = memberService;
        this.currentUserService = currentUserService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public String getAllChats(Model model) {
        addChatSidebarModel(model);
        return "chat-overview";
    }

    @PostMapping("/start")
    public String startChat(@RequestParam("memberId") UUID memberId) {
        Conversation conversation = conversationService.findOrCreateConversation(memberId);
        return "redirect:/chat/" + conversation.getConversationId();
    }

    @GetMapping("/groups/create")
    public String createGroupChatPlaceholder(Model model) {
        addChatSidebarModel(model);
        model.addAttribute("groupChatMembers", getGroupChatMembers());
        model.addAttribute("createGroupChatPlaceholder", true);
        return "chat-overview";
    }

    @PostMapping("/groups/create")
    public String createGroupChat(@RequestParam String title,
                                  @RequestParam(required = false) List<UUID> memberIds,
                                  RedirectAttributes redirectAttributes) {
        try {
            Conversation conversation = conversationService.createCustomGroupConversation(title, memberIds);
            return "redirect:/chat/" + conversation.getConversationId();
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("toastType", "info");
            redirectAttributes.addFlashAttribute("toastMessage", exception.getReason());
            return "redirect:/chat/groups/create";
        }
    }

    @PostMapping("/groups/travel/{groupId}/open")
    public String openTravelGroupChat(@PathVariable UUID groupId) {
        Conversation conversation = conversationService.openTravelGroupConversation(groupId);
        return "redirect:/chat/" + conversation.getConversationId();
    }

    @GetMapping("/contacts")
    public String getContacts(Model model) {
        Member currentMember = currentUserService.getCurrentUser();

        List<Member> members = memberService.findAll()
                .stream()
                .filter(member -> !member.getUserId().equals(currentMember.getUserId()))
                .toList();

        model.addAttribute("members", members);
        return "contacts";
    }

    @GetMapping("/{conversationId}")
    public String getChat(@PathVariable UUID conversationId,
                          @RequestParam(defaultValue = "false") boolean groupInfo,
                          Model model) {
        addChatSidebarModel(model);

        List<Message> messages = messageService.getMessagesByConversation(conversationId);
        Set<UUID> seenMessageIds = messageService.getSeenMessageIdsForCurrentMember(conversationId, messages);
        List<Conversation> allConversations = conversationService.findAllForCurrentMember();

        Conversation selectedConversationDetails = allConversations.stream()
                .filter(conversation -> conversation.getConversationId().equals(conversationId))
                .findFirst()
                .orElse(null);

        String selectedConversationName = selectedConversationDetails == null
                ? "Conversation"
                : conversationService.getConversationDisplayName(selectedConversationDetails);
        boolean selectedGroupChat = selectedConversationDetails != null
                && selectedConversationDetails.getType() != ConversationType.DIRECT;

        model.addAttribute("messages", messages);
        model.addAttribute("seenMessageIds", seenMessageIds);
        model.addAttribute("conversationId", conversationId);
        model.addAttribute("currentMember", currentUserService.getCurrentUser());
        model.addAttribute("selectedConversationName", selectedConversationName);
        model.addAttribute("selectedConversation", true);
        model.addAttribute("selectedGroupChat", selectedGroupChat);
        if (groupInfo && selectedGroupChat) {
            boolean customGroupChat = selectedConversationDetails.getType() == ConversationType.CUSTOM_GROUP;
            boolean customGroupOwner = customGroupChat
                    && conversationService.isCurrentMemberCustomGroupOwner(conversationId);
            List<MemberConversation> groupMembers = conversationService.findGroupMembersForCurrentMember(conversationId);

            model.addAttribute("showGroupInfo", true);
            model.addAttribute("customGroupChat", customGroupChat);
            model.addAttribute("customGroupOwner", customGroupOwner);
            model.addAttribute("customGroupOnlyMember", customGroupChat && groupMembers.size() == 1);
            model.addAttribute("groupMembers", groupMembers);
            if (customGroupOwner) {
                model.addAttribute(
                        "availableGroupMembers",
                        conversationService.findAvailableMembersForCustomGroup(conversationId)
                );
            }
        }
        return "chat-overview";
    }

    @PostMapping("/{conversationId}/groups/rename")
    public String renameCustomGroup(@PathVariable UUID conversationId,
                                    @RequestParam String title,
                                    RedirectAttributes redirectAttributes) {
        try {
            conversationService.renameCustomGroup(conversationId, title);
            addSuccessToast(redirectAttributes, message("flash.chat.groupRenamed"));
        } catch (ResponseStatusException exception) {
            addInfoToast(redirectAttributes, exception.getReason());
        }
        return redirectToGroupInfo(conversationId);
    }

    @PostMapping("/{conversationId}/groups/members")
    public String addCustomGroupMember(@PathVariable UUID conversationId,
                                       @RequestParam UUID memberId,
                                       RedirectAttributes redirectAttributes) {
        try {
            conversationService.addMemberToCustomGroup(conversationId, memberId);
            addSuccessToast(redirectAttributes, message("flash.chat.memberAdded"));
        } catch (ResponseStatusException exception) {
            addInfoToast(redirectAttributes, exception.getReason());
        }
        return redirectToGroupInfo(conversationId);
    }

    @PostMapping("/{conversationId}/groups/members/{memberId}/remove")
    public String removeCustomGroupMember(@PathVariable UUID conversationId,
                                          @PathVariable UUID memberId,
                                          RedirectAttributes redirectAttributes) {
        try {
            conversationService.removeMemberFromCustomGroup(conversationId, memberId);
            addSuccessToast(redirectAttributes, message("flash.chat.memberRemoved"));
        } catch (ResponseStatusException exception) {
            addInfoToast(redirectAttributes, exception.getReason());
        }
        return redirectToGroupInfo(conversationId);
    }

    @PostMapping("/{conversationId}/groups/members/{memberId}/role")
    public String updateCustomGroupMemberRole(@PathVariable UUID conversationId,
                                              @PathVariable UUID memberId,
                                              @RequestParam MemberConversationRole role,
                                              RedirectAttributes redirectAttributes) {
        try {
            conversationService.updateCustomGroupMemberRole(conversationId, memberId, role);
            addSuccessToast(redirectAttributes, message("flash.chat.memberRoleUpdated"));
        } catch (ResponseStatusException exception) {
            addInfoToast(redirectAttributes, exception.getReason());
        }
        return redirectToGroupInfo(conversationId);
    }

    @PostMapping("/{conversationId}/groups/leave")
    public String leaveCustomGroup(@PathVariable UUID conversationId,
                                   RedirectAttributes redirectAttributes) {
        try {
            conversationService.leaveCustomGroup(conversationId);
            addSuccessToast(redirectAttributes, message("flash.chat.groupLeft"));
            return "redirect:/chat";
        } catch (ResponseStatusException exception) {
            addInfoToast(redirectAttributes, exception.getReason());
            return redirectToGroupInfo(conversationId);
        }
    }

    @PostMapping("/{conversationId}/groups/delete")
    public String deleteCustomGroup(@PathVariable UUID conversationId,
                                    RedirectAttributes redirectAttributes) {
        try {
            conversationService.deleteCustomGroupIfOnlyMember(conversationId);
            addSuccessToast(redirectAttributes, message("flash.chat.groupDeleted"));
            return "redirect:/chat";
        } catch (ResponseStatusException exception) {
            addInfoToast(redirectAttributes, exception.getReason());
            return redirectToGroupInfo(conversationId);
        }
    }

    @PostMapping("/{conversationId}/send")
    public String sendMessage(@PathVariable UUID conversationId,
                              @RequestParam(required = false) String message,
                              @RequestParam(required = false) MultipartFile image,
                              RedirectAttributes redirectAttributes) {
        try {
            messageService.sendMessage(conversationId, message, image);
        } catch (ResponseStatusException exception) {
            addInfoToast(redirectAttributes, exception.getReason());
        }
        return "redirect:/chat/" + conversationId;
    }

    @GetMapping("/messages/{messageId}/image")
    public ResponseEntity<byte[]> getMessageImage(@PathVariable UUID messageId) {
        MessageService.MessageImage image = messageService.getMessageImageForCurrentMember(messageId);
        MediaType mediaType = image.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(image.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(image.fileName())
                        .build()
                        .toString())
                .body(image.data());
    }

    private void addChatSidebarModel(Model model) {
        Member currentMember = currentUserService.getCurrentUser();

        List<Conversation> conversations = conversationService.findDirectConversationsForCurrentMember();
        List<Conversation> groupConversations = conversationService.findGroupConversationsForCurrentMember();

        model.addAttribute("groupConversations", groupConversations);
        model.addAttribute("conversations", conversations);
        model.addAttribute("members", getAvailableMembers(currentMember, conversations));
        model.addAttribute("currentMember", currentMember);
        model.addAttribute("selectedConversation", false);
        model.addAttribute("selectedGroupChat", false);
        model.addAttribute("createGroupChatPlaceholder", false);
    }

    private List<Member> getAvailableMembers(Member currentMember, List<Conversation> conversations) {
        List<UUID> existingConversationMemberIds = conversations.stream()
                .map(conversationService::getOtherMemberId)
                .filter(Objects::nonNull)
                .toList();

        return memberService.findAll()
                .stream()
                .filter(member -> !member.getUserId().equals(currentMember.getUserId()))
                .filter(member -> !existingConversationMemberIds.contains(member.getUserId()))
                .toList();
    }

    private List<Member> getGroupChatMembers() {
        Member currentMember = currentUserService.getCurrentUser();

        return memberService.findAll()
                .stream()
                .filter(member -> !member.getUserId().equals(currentMember.getUserId()))
                .toList();
    }

    private String redirectToGroupInfo(UUID conversationId) {
        return "redirect:/chat/" + conversationId + "?groupInfo=true";
    }

    private void addSuccessToast(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("toastType", "success");
        redirectAttributes.addFlashAttribute("toastMessage", message);
    }

    private void addInfoToast(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("toastType", "info");
        redirectAttributes.addFlashAttribute("toastMessage", message);
    }

    private String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }
}