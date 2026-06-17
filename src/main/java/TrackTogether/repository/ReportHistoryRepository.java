package TrackTogether.repository;

import TrackTogether.domain.Conversation;
import TrackTogether.domain.ReportHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportHistoryRepository extends JpaRepository<ReportHistoryEntry, UUID> {

    void deleteAllByReport_Message_Conversation(Conversation conversation);

    /** All history entries with their lazy {@code changedBy} user loaded. */
    @Query("SELECT e FROM ReportHistoryEntry e LEFT JOIN FETCH e.changedBy ORDER BY e.changedAt DESC")
    List<ReportHistoryEntry> findAllByOrderByChangedAtDesc();

    /** History for a single report with its lazy {@code changedBy} user loaded. */
    @Query("SELECT e FROM ReportHistoryEntry e LEFT JOIN FETCH e.changedBy WHERE e.report.reportId = :reportId ORDER BY e.changedAt DESC")
    List<ReportHistoryEntry> findByReport_ReportIdOrderByChangedAtDesc(@Param("reportId") UUID reportId);
}