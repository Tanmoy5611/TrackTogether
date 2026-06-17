package TrackTogether.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "delijn.api")
public class DeLijnProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.delijn.be";
    private String apiKey;
    private Duration timeout = Duration.ofSeconds(3);
    private int nearbyStopRadiusMeters = 2500;
    private int maxNearbyStops = 20;
    private int maxDepartures = 8;
    private Endpoints endpoints = new Endpoints();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getNearbyStopRadiusMeters() {
        return nearbyStopRadiusMeters;
    }

    public void setNearbyStopRadiusMeters(int nearbyStopRadiusMeters) {
        this.nearbyStopRadiusMeters = nearbyStopRadiusMeters;
    }

    public int getMaxNearbyStops() {
        return maxNearbyStops;
    }

    public void setMaxNearbyStops(int maxNearbyStops) {
        this.maxNearbyStops = maxNearbyStops;
    }

    public int getMaxDepartures() {
        return maxDepartures;
    }

    public void setMaxDepartures(int maxDepartures) {
        this.maxDepartures = maxDepartures;
    }

    public Endpoints getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Endpoints endpoints) {
        this.endpoints = endpoints;
    }

    public static class Endpoints {
        private String nearbyStops = "";
        private String realtimeDepartures = "/DLKernOpenData/api/v1/haltes/{entityNumber}/{stopNumber}/real-time?maxAantalDoorkomsten={maxDepartures}";
        private String scheduledDepartures = "/DLKernOpenData/api/v1/haltes/{entityNumber}/{stopNumber}/dienstregelingen?datum={date}";
        private String stopDetails = "/DLKernOpenData/api/v1/haltes/{entityNumber}/{stopNumber}";
        private String searchStops = "/DLZoekOpenData/v1/zoek/haltes/{query}?huidigePositie={position}&maxAantalHits={maxResults}";
        private String routeOptions = "";

        public String getNearbyStops() {
            return nearbyStops;
        }

        public void setNearbyStops(String nearbyStops) {
            this.nearbyStops = nearbyStops;
        }

        public String getRealtimeDepartures() {
            return realtimeDepartures;
        }

        public void setRealtimeDepartures(String realtimeDepartures) {
            this.realtimeDepartures = realtimeDepartures;
        }

        public String getScheduledDepartures() {
            return scheduledDepartures;
        }

        public void setScheduledDepartures(String scheduledDepartures) {
            this.scheduledDepartures = scheduledDepartures;
        }

        public String getStopDetails() {
            return stopDetails;
        }

        public void setStopDetails(String stopDetails) {
            this.stopDetails = stopDetails;
        }

        public String getSearchStops() {
            return searchStops;
        }

        public void setSearchStops(String searchStops) {
            this.searchStops = searchStops;
        }

        public String getRouteOptions() {
            return routeOptions;
        }

        public void setRouteOptions(String routeOptions) {
            this.routeOptions = routeOptions;
        }
    }
}