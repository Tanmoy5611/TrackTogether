package TrackTogether.webapi.dto;

import java.util.Set;
import java.util.UUID;

public record ChatRealtimeReadReceiptDto(
        String type,
        Set<UUID> seenMessageIds
) {
}