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

    // Returns whether De Lijn integration is enabled
    public boolean isEnabled() {
        return enabled;
    }

    // Sets whether De Lijn integration is enabled
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Returns the De Lijn API base URL
    public String getBaseUrl() {
        return baseUrl;
    }

    // Sets the De Lijn API base URL
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // Returns the configured De Lijn subscription key
    public String getApiKey() {
        return apiKey;
    }

    // Sets the De Lijn subscription key
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    // Returns the REST timeout for De Lijn calls
    public Duration getTimeout() {
        return timeout;
    }

    // Sets the REST timeout for De Lijn calls
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    // Returns the default search radius for nearby stops
    public int getNearbyStopRadiusMeters() {
        return nearbyStopRadiusMeters;
    }

    // Sets the default search radius for nearby stops
    public void setNearbyStopRadiusMeters(int nearbyStopRadiusMeters) {
        this.nearbyStopRadiusMeters = nearbyStopRadiusMeters;
    }

    // Returns the default maximum number of nearby stops
    public int getMaxNearbyStops() {
        return maxNearbyStops;
    }

    // Sets the default maximum number of nearby stops
    public void setMaxNearbyStops(int maxNearbyStops) {
        this.maxNearbyStops = maxNearbyStops;
    }

    // Returns the default maximum number of departures
    public int getMaxDepartures() {
        return maxDepartures;
    }

    // Sets the default maximum number of departures
    public void setMaxDepartures(int maxDepartures) {
        this.maxDepartures = maxDepartures;
    }

    // Returns configured De Lijn endpoint paths
    public Endpoints getEndpoints() {
        return endpoints;
    }

    // Sets configured De Lijn endpoint paths
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

        // Returns the nearby stops endpoint path
        public String getNearbyStops() {
            return nearbyStops;
        }

        // Sets the nearby stops endpoint path
        public void setNearbyStops(String nearbyStops) {
            this.nearbyStops = nearbyStops;
        }

        // Returns the real-time departures endpoint path
        public String getRealtimeDepartures() {
            return realtimeDepartures;
        }

        // Sets the real-time departures endpoint path
        public void setRealtimeDepartures(String realtimeDepartures) {
            this.realtimeDepartures = realtimeDepartures;
        }

        // Returns the scheduled departures endpoint path
        public String getScheduledDepartures() {
            return scheduledDepartures;
        }

        // Sets the scheduled departures endpoint path
        public void setScheduledDepartures(String scheduledDepartures) {
            this.scheduledDepartures = scheduledDepartures;
        }

        // Returns the stop details endpoint path
        public String getStopDetails() {
            return stopDetails;
        }

        // Sets the stop details endpoint path
        public void setStopDetails(String stopDetails) {
            this.stopDetails = stopDetails;
        }

        // Returns the stop search endpoint path
        public String getSearchStops() {
            return searchStops;
        }

        // Sets the stop search endpoint path
        public void setSearchStops(String searchStops) {
            this.searchStops = searchStops;
        }

        // Returns the route planner endpoint path
        public String getRouteOptions() {
            return routeOptions;
        }

        // Sets the route planner endpoint path
        public void setRouteOptions(String routeOptions) {
            this.routeOptions = routeOptions;
        }
    }
}