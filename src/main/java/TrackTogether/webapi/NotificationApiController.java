package TrackTogether.webapi;

import TrackTogether.domain.Notification;
import TrackTogether.service.NotificationService;
import TrackTogether.webapi.dto.NotificationDto;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final MessageSource messageSource;

    public NotificationApiController(NotificationService notificationService,
                                     MessageSource messageSource) {
        this.notificationService = notificationService;
        this.messageSource = messageSource;
    }

    @GetMapping
    public List<NotificationDto> getNotifications() {
        return notificationService.getNotificationsForCurrentUser()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount() {
        return Map.of("count", notificationService.getUnreadCountForCurrentUser());
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }

    @PutMapping("/read-all")
    public void markAllAsRead() {
        notificationService.markAllAsRead();
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType().name(),
                buildMessage(n),
                n.getGroupId(),
                n.getCreatedAt(),
                n.isRead()
        );
    }

    private String buildMessage(Notification n) {
        String key = "notification." + n.getType().name();
        if (n.getActorName() == null) {
            return message(key, n.getGroupLocation());
        }
        return message(key, n.getActorName(), n.getGroupLocation());
    }

    private String message(String key, Object... arguments) {
        return messageSource.getMessage(key, arguments, LocaleContextHolder.getLocale());
    }
}