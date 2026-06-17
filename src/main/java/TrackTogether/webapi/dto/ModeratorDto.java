package TrackTogether.webapi.dto;

import java.util.UUID;

public record ModeratorDto(
        UUID moderatorId,
        String name
) {
}