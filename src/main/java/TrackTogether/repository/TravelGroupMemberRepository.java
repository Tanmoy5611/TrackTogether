package TrackTogether.repository;

import TrackTogether.domain.Member;
import TrackTogether.domain.TravelGroup;
import TrackTogether.domain.TravelGroupMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TravelGroupMemberRepository extends JpaRepository<TravelGroupMember, Integer> {

    @Override
    // Loads the group and activity together because analytics uses both values
    @EntityGraph(attributePaths = {"group", "group.activity"})
    List<TravelGroupMember> findAll();

    boolean existsByGroupAndMember(TravelGroup group, Member member);

    long countByGroup(TravelGroup group);

    // Gets all joined group ids in one query instead of checking every card separately
    @Query("""
            select membership.group.groupId
            from TravelGroupMember membership
            where membership.member = :member
              and membership.group in :groups
            """)
    Set<UUID> findGroupIdsByMemberAndGroupIn(@Param("member") Member member,
                                             @Param("groups") List<TravelGroup> groups);

    // Counts members for all listed groups at once for the travel group cards
    @Query("""
            select membership.group.groupId, count(membership)
            from TravelGroupMember membership
            where membership.group in :groups
            group by membership.group.groupId
            """)
    List<Object[]> countMembersByGroupIn(@Param("groups") List<TravelGroup> groups);

    @EntityGraph(attributePaths = {"member", "location"})
    Optional<TravelGroupMember> findByGroupAndMember(TravelGroup group, Member member);

    @EntityGraph(attributePaths = {"member", "location"})
    List<TravelGroupMember> findAllByGroup(TravelGroup group);

    // Loads the joined groups with their activities for dashboard and analytics pages
    @EntityGraph(attributePaths = {"group", "group.activity"})
    List<TravelGroupMember> findAllByMember(Member member);

    long countByMember(Member member);
}