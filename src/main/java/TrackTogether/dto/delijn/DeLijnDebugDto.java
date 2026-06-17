package TrackTogether.dto.delijn;

public class DeLijnDebugDto {

    private final boolean configured;
    private final String requestPath;
    private final Integer statusCode;
    private final int parsedStopCount;
    private final int parsedDepartureCount;
    private final String bodyPreview;
    private final String error;

    public DeLijnDebugDto(boolean configured,
                          String requestPath,
                          Integer statusCode,
                          int parsedStopCount,
                          String bodyPreview,
                          String error) {
        this(configured, requestPath, statusCode, parsedStopCount, 0, bodyPreview, error);
    }

    public DeLijnDebugDto(boolean configured,
                          String requestPath,
                          Integer statusCode,
                          int parsedStopCount,
                          int parsedDepartureCount,
                          String bodyPreview,
                          String error) {
        this.configured = configured;
        this.requestPath = requestPath;
        this.statusCode = statusCode;
        this.parsedStopCount = parsedStopCount;
        this.parsedDepartureCount = parsedDepartureCount;
        this.bodyPreview = bodyPreview;
        this.error = error;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public int getParsedStopCount() {
        return parsedStopCount;
    }

    public int getParsedDepartureCount() {
        return parsedDepartureCount;
    }

    public String getBodyPreview() {
        return bodyPreview;
    }

    public String getError() {
        return error;
    }
}