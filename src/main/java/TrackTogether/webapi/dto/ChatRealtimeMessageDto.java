package TrackTogether.webapi.dto;

import java.util.UUID;

public record ChatRealtimeMessageDto(
        String type,
        UUID messageId,
        UUID senderId,
        String senderName,
        String message,
        String imageUrl,
        String imageFileName,
        String timestamp,
        boolean seen
) {
}