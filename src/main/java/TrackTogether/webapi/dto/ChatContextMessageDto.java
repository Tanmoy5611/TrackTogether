package TrackTogether.webapi.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatContextMessageDto(
        UUID messageId,
        String senderName,
        String content,
        LocalDateTime timestamp,
        boolean flagged
) {
}