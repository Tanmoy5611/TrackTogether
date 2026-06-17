package TrackTogether.webapi.mapper;

import TrackTogether.domain.TravelGroup;
import TrackTogether.webapi.dto.TravelGroupDto;
import org.springframework.stereotype.Component;

// Mapper converts domain entity - API DTO
@Component
public class TravelGroupMapper {

    // Convert TravelGroup entity to TravelGroupDto
    public TravelGroupDto toDto(TravelGroup group) {

        // Prevent null pointer issues
        if (group == null) {
            return null;
        }

        // Map fields manually
        return new TravelGroupDto(
                group.getGroupId(),
                group.getMaxMembers(),
                group.getLocation(),
                group.getDepartureLocation(),
                group.getDepartureLatitude(),
                group.getDepartureLongitude(),
                group.getArrivalLatitude(),
                group.getArrivalLongitude(),
                group.getDepartureTime(),
                group.getEstimatedArrivalTime(),
                group.getTransportMode(),
                group.getOwner() != null ? group.getOwner().getUserId() : null,
                group.getOwner() != null ? group.getOwner().getName() : null
        );

    }
}