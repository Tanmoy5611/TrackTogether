package TrackTogether.exceptions;

import java.util.UUID;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException foUserOriginalId(final String id) {
        return new NotFoundException("User #" + id + " not found.");
    }

    public static NotFoundException forActivityId(final String id) {
        return new NotFoundException("Activity #" + id + " not found.");
    }

    public static NotFoundException forMessageId(final UUID id){
        return new NotFoundException("Message #" + id + " not found.");
    }
}