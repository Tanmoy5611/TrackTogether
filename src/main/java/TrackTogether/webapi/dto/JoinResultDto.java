package TrackTogether.webapi.dto;

public record JoinResultDto(
        long memberCount,
        int maxMembers,
        boolean joined,
        boolean pendingApproval,
        String message
) {}