package TrackTogether.service;

import TrackTogether.domain.Conversation;
import TrackTogether.domain.Member;
import TrackTogether.domain.MemberConversation;
import TrackTogether.domain.Message;
import TrackTogether.exceptions.NotFoundException;
import TrackTogether.repository.ConversationRepository;
import TrackTogether.repository.MemberConversationRepository;
import TrackTogether.repository.MessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class MessageService {
    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MemberConversationRepository memberConversationRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final ChatRealtimeService chatRealtimeService;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          MemberConversationRepository memberConversationRepository,
                          CurrentUserService currentUserService,
                          NotificationService notificationService,
                          ChatRealtimeService chatRealtimeService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.memberConversationRepository = memberConversationRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.chatRealtimeService = chatRealtimeService;
    }

    @Transactional
    public List<Message> getMessagesByConversation(UUID conversationId) {
        Conversation conversation = getConversationForCurrentMember(conversationId);
        List<Message> messages = messageRepository.findByConversationIdWithSender(conversationId);
        markConversationRead(conversation, currentUserService.getCurrentUser());
        return messages;
    }

    @Transactional(readOnly = true)
    public Set<UUID> getSeenMessageIdsForCurrentMember(UUID conversationId, List<Message> messages) {
        Conversation conversation = getConversationForCurrentMember(conversationId);
        Member currentMember = currentUserService.getCurrentUser();
        List<MemberConversation> participants = memberConversationRepository.findAllByConversation(conversation);

        return messages.stream()
                .filter(message -> message.getSender().getUserId().equals(currentMember.getUserId()))
                .filter(message -> isSeenByEveryOtherParticipant(message, participants))
                .map(Message::getMessageId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void sendMessage(UUID conversationId, String messageText) {
        sendMessage(conversationId, messageText, null);
    }

    @Transactional
    public void sendMessage(UUID conversationId, String messageText, MultipartFile image) {
        Conversation conversation = getConversationForCurrentMember(conversationId);
        boolean hasText = messageText != null && !messageText.isBlank();
        boolean hasImage = image != null && !image.isEmpty();

        if (!hasText && !hasImage) {
            return;
        }

        Member sender = currentUserService.getCurrentUser();

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessage(hasText ? messageText.trim() : "");
        message.setTimeStamp(LocalDateTime.now());
        if (hasImage) {
            attachImage(message, image);
        }

        Message savedMessage = messageRepository.save(message);
        notificationService.notifyNewMessage(sender, conversation);
        broadcastAfterCommit(savedMessage);
    }

    @Transactional(readOnly = true)
    public MessageImage getMessageImageForCurrentMember(UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message #" + messageId + " not found."));

        Conversation conversation = message.getConversation();
        Member currentMember = currentUserService.getCurrentUser();
        if (!memberConversationRepository.existsByConversationAndMember(conversation, currentMember)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this conversation");
        }
        if (!message.hasImage()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }

        return new MessageImage(
                message.getImageData(),
                message.getImageContentType(),
                message.getImageFileName()
        );
    }

    private Conversation getConversationForCurrentMember(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation #" + conversationId + " not found."));

        Member currentMember = currentUserService.getCurrentUser();
        if (!memberConversationRepository.existsByConversationAndMember(conversation, currentMember)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this conversation");
        }

        return conversation;
    }

    private void markConversationRead(Conversation conversation, Member currentMember) {
        memberConversationRepository.findByConversationAndMember(conversation, currentMember)
                .ifPresent(memberConversation -> memberConversation.setLastReadAt(LocalDateTime.now()));
    }

    private void attachImage(Message message, MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files can be sent");
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Images must be 5 MB or smaller");
        }

        try {
            message.setImageData(image.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read image file");
        }

        message.setImageContentType(contentType);
        message.setImageFileName(cleanFileName(image.getOriginalFilename()));
    }

    private String cleanFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "image";
        }

        return fileName.replace("\\", "_").replace("/", "_").replace("\"", "_");
    }

    private boolean isSeenByEveryOtherParticipant(Message message, List<MemberConversation> participants) {
        return participants.stream()
                .filter(participant -> !participant.getMember().getUserId().equals(message.getSender().getUserId()))
                .allMatch(participant -> participant.getLastReadAt() != null
                        && !participant.getLastReadAt().isBefore(message.getTimeStamp()));
    }

    private void broadcastAfterCommit(Message message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            chatRealtimeService.broadcast(message);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatRealtimeService.broadcast(message);
            }
        });
    }

    public record MessageImage(byte[] data, String contentType, String fileName) {
    }
}