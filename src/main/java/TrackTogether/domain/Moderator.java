package TrackTogether.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class Moderator extends Member {

    @OneToMany(mappedBy = "moderator", fetch = FetchType.LAZY)
    private List<Report> reports = new ArrayList<>();

    public Moderator(Boolean status, String email, String name, UUID userId, Double CO2Saved) {
        super(status, email, name, userId, CO2Saved);
    }

    public Moderator() {
        super();
    }

    public List<Report> getReports() {
        return reports;
    }

    public void setReports(List<Report> reports) {
        this.reports = reports;
    }
}