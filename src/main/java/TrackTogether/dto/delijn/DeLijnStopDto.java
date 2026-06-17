package TrackTogether.dto.delijn;

public class DeLijnStopDto {

    private final String entityNumber;
    private final String stopNumber;
    private final String name;
    private final String municipality;
    private final Double latitude;
    private final Double longitude;
    private final Integer distanceMeters;

    public DeLijnStopDto(String entityNumber,
                         String stopNumber,
                         String name,
                         String municipality,
                         Double latitude,
                         Double longitude,
                         Integer distanceMeters) {
        this.entityNumber = entityNumber;
        this.stopNumber = stopNumber;
        this.name = name;
        this.municipality = municipality;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
    }

    public String getEntityNumber() {
        return entityNumber;
    }

    public String getStopNumber() {
        return stopNumber;
    }

    public String getName() {
        return name;
    }

    public String getMunicipality() {
        return municipality;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }
}