package com.aiinsight.domain.topic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterestTopicRepository extends JpaRepository<InterestTopic, Long> {

    List<InterestTopic> findByUserIdAndIsActiveTrueOrderByDisplayOrderAsc(Long userId);

    List<InterestTopic> findByUserIdOrderByDisplayOrderAsc(Long userId);

    Optional<InterestTopic> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndNameAndIdNot(Long userId, String name, Long id);

    @Query("SELECT COALESCE(MAX(t.displayOrder), 0) FROM InterestTopic t WHERE t.user.id = :userId")
    Integer findMaxDisplayOrderByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE InterestTopic t SET t.displayOrder = t.displayOrder - 1 " +
           "WHERE t.user.id = :userId AND t.displayOrder > :deletedOrder")
    void decrementDisplayOrderAfter(@Param("userId") Long userId, @Param("deletedOrder") Integer deletedOrder);

    long countByUserId(Long userId);
}
