package TrackTogether.service;

import TrackTogether.domain.Conversation;
import TrackTogether.domain.Member;
import TrackTogether.domain.MemberConversation;
import TrackTogether.domain.Notification;
import TrackTogether.domain.NotificationType;
import TrackTogether.domain.TravelGroup;
import TrackTogether.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository,
                                CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    public void notifyMemberJoined(Member recipient, Member actor, TravelGroup group) {
        if (recipient.getUserId().equals(actor.getUserId())) {
            return;
        }
        notificationRepository.save(new Notification(
                recipient, NotificationType.MEMBER_JOINED,
                actor.getName(), group.getLocation(), group.getGroupId()
        ));
    }

    public void notifyGroupFull(List<Member> members, TravelGroup group) {
        List<Notification> notifications = members.stream()
                .map(member -> new Notification(
                        member, NotificationType.GROUP_FULL, null, group.getLocation(), group.getGroupId()
                ))
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
    }

    public void notifyJoinRequestReceived(Member owner, Member requester, TravelGroup group) {
        notificationRepository.save(new Notification(
                owner, NotificationType.JOIN_REQUEST_RECEIVED,
                requester.getName(), group.getLocation(), group.getGroupId()
        ));
    }

    public void notifyJoinRequestAccepted(Member requester, TravelGroup group) {
        notificationRepository.save(new Notification(
                requester, NotificationType.JOIN_REQUEST_ACCEPTED,
                null, group.getLocation(), group.getGroupId()
        ));
    }

    public void notifyJoinRequestRejected(Member requester, TravelGroup group) {
        notificationRepository.save(new Notification(
                requester, NotificationType.JOIN_REQUEST_REJECTED,
                null, group.getLocation(), group.getGroupId()
        ));
    }

    public void notifyNewMessage(Member sender, Conversation conversation) {
        TravelGroup group = conversation.getTravelGroup();
        if (group == null) return;
        List<Notification> notifications = conversation.getMembers().stream()
                .map(MemberConversation::getMember)
                .filter(m -> !m.getUserId().equals(sender.getUserId()))
                .map(recipient -> new Notification(
                        recipient, NotificationType.NEW_MESSAGE,
                        sender.getName(), group.getLocation(), group.getGroupId()
                ))
                .collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
    }

    public void notifyMemberLeft(Member recipient, Member actor, TravelGroup group) {
        if (recipient.getUserId().equals(actor.getUserId())) {
            return;
        }
        notificationRepository.save(new Notification(
                recipient, NotificationType.MEMBER_LEFT,
                actor.getName(), group.getLocation(), group.getGroupId()
        ));
    }

    public List<Notification> getNotificationsForCurrentUser() {
        Member member = currentUserService.getCurrentUser();
        return notificationRepository.findTop20ByRecipientOrderByCreatedAtDesc(member);
    }

    public long getUnreadCountForCurrentUser() {
        Member member = currentUserService.getCurrentUser();
        return notificationRepository.countByRecipientAndReadFalse(member);
    }

    public void markAsRead(Long notificationId) {
        Member member = currentUserService.getCurrentUser();
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipient().getUserId().equals(member.getUserId())) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    public void markAllAsRead() {
        Member member = currentUserService.getCurrentUser();
        notificationRepository.markAllAsReadForRecipient(member);
    }
}