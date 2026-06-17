package TrackTogether.service;

import TrackTogether.domain.Conversation;
import TrackTogether.domain.Member;
import TrackTogether.domain.MemberConversation;
import TrackTogether.domain.Message;
import TrackTogether.repository.ConversationRepository;
import TrackTogether.repository.MemberConversationRepository;
import TrackTogether.repository.MemberRepository;
import TrackTogether.repository.MessageRepository;
import TrackTogether.webapi.dto.ChatRealtimeMessageDto;
import TrackTogether.webapi.dto.ChatRealtimeReadReceiptDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatRealtimeService {

    private final ConversationRepository conversationRepository;
    private final MemberConversationRepository memberConversationRepository;
    private final MemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final Map<UUID, Set<WebSocketSession>> sessionsByConversation = new ConcurrentHashMap<>();
    private final Map<String, UUID> conversationBySessionId = new ConcurrentHashMap<>();
    private final Map<String, UUID> memberBySessionId = new ConcurrentHashMap<>();

    public ChatRealtimeService(ConversationRepository conversationRepository,
                               MemberConversationRepository memberConversationRepository,
                               MemberRepository memberRepository,
                               MessageRepository messageRepository,
                               ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.memberConversationRepository = memberConversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean register(UUID conversationId, WebSocketSession session) {
        Optional<Member> currentMember = findCurrentMember(session.getPrincipal());
        if (currentMember.isEmpty()) {
            return false;
        }

        Optional<Conversation> conversation = conversationRepository.findById(conversationId);
        if (conversation.isEmpty()
                || !memberConversationRepository.existsByConversationAndMember(conversation.get(), currentMember.get())) {
            return false;
        }

        sessionsByConversation
                .computeIfAbsent(conversationId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
        conversationBySessionId.put(session.getId(), conversationId);
        memberBySessionId.put(session.getId(), currentMember.get().getUserId());
        markConversationRead(conversation.get(), currentMember.get());
        broadcastSeenStatuses(conversation.get());
        return true;
    }

    public void unregister(WebSocketSession session) {
        UUID conversationId = conversationBySessionId.remove(session.getId());
        memberBySessionId.remove(session.getId());
        if (conversationId == null) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByConversation.get(conversationId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByConversation.remove(conversationId);
        }
    }

    @Transactional
    public void handleClientMessage(WebSocketSession session, String payload) {
        if (!isSeenPayload(payload)) {
            return;
        }

        UUID conversationId = conversationBySessionId.get(session.getId());
        UUID memberId = memberBySessionId.get(session.getId());
        if (conversationId == null || memberId == null) {
            return;
        }

        Optional<Conversation> conversation = conversationRepository.findById(conversationId);
        Optional<Member> member = memberRepository.findById(memberId);
        if (conversation.isEmpty() || member.isEmpty()) {
            return;
        }

        markConversationRead(conversation.get(), member.get());
        broadcastSeenStatuses(conversation.get());
    }

    public void broadcast(Message message) {
        UUID conversationId = message.getConversation().getConversationId();
        Set<WebSocketSession> sessions = sessionsByConversation.get(conversationId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(toDto(message));
        } catch (JsonProcessingException exception) {
            return;
        }

        TextMessage textMessage = new TextMessage(payload);
        sessions.removeIf(session -> !send(session, textMessage));
        if (sessions.isEmpty()) {
            sessionsByConversation.remove(conversationId);
        }
    }

    private Optional<Member> findCurrentMember(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return Optional.empty();
        }

        return memberRepository.findByOriginalId("GOOGLE-" + principal.getName());
    }

    private boolean isSeenPayload(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            return "seen".equals(root.path("type").asText());
        } catch (IOException exception) {
            return false;
        }
    }

    private void markConversationRead(Conversation conversation, Member member) {
        memberConversationRepository.findByConversationAndMember(conversation, member)
                .ifPresent(memberConversation -> memberConversation.setLastReadAt(LocalDateTime.now()));
    }

    private void broadcastSeenStatuses(Conversation conversation) {
        Set<UUID> seenMessageIds = getSeenMessageIds(conversation);
        if (seenMessageIds.isEmpty()) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(
                    new ChatRealtimeReadReceiptDto("seen", seenMessageIds)
            );
        } catch (JsonProcessingException exception) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByConversation.get(conversation.getConversationId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        TextMessage textMessage = new TextMessage(payload);
        sessions.removeIf(session -> !send(session, textMessage));
        if (sessions.isEmpty()) {
            sessionsByConversation.remove(conversation.getConversationId());
        }
    }

    private Set<UUID> getSeenMessageIds(Conversation conversation) {
        List<MemberConversation> participants = memberConversationRepository.findAllByConversation(conversation);

        return messageRepository.findByConversationIdWithSender(conversation.getConversationId())
                .stream()
                .filter(message -> isSeenByEveryOtherParticipant(message, participants))
                .map(Message::getMessageId)
                .collect(Collectors.toSet());
    }

    private boolean isSeenByEveryOtherParticipant(Message message, List<MemberConversation> participants) {
        return participants.stream()
                .filter(participant -> !participant.getMember().getUserId().equals(message.getSender().getUserId()))
                .allMatch(participant -> participant.getLastReadAt() != null
                        && !participant.getLastReadAt().isBefore(message.getTimeStamp()));
    }

    private boolean send(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            conversationBySessionId.remove(session.getId());
            memberBySessionId.remove(session.getId());
            return false;
        }

        try {
            synchronized (session) {
                session.sendMessage(message);
            }
            return true;
        } catch (IOException exception) {
            conversationBySessionId.remove(session.getId());
            memberBySessionId.remove(session.getId());
            return false;
        }
    }

    private static ChatRealtimeMessageDto toDto(Message message) {
        return new ChatRealtimeMessageDto(
                "message",
                message.getMessageId(),
                message.getSender().getUserId(),
                message.getSender().getName(),
                message.getMessage(),
                message.hasImage() ? "/chat/messages/" + message.getMessageId() + "/image" : null,
                message.getImageFileName(),
                message.getTimeStamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                false
        );
    }
}