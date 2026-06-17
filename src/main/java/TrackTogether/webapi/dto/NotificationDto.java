package TrackTogether.webapi.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class NotificationDto {

    private final Long id;
    private final String type;
    private final String message;
    private final UUID groupId;
    private final LocalDateTime createdAt;
    private final boolean read;

    public NotificationDto(Long id, String type, String message,
                           UUID groupId, LocalDateTime createdAt, boolean read) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.groupId = groupId;
        this.createdAt = createdAt;
        this.read = read;
    }
}