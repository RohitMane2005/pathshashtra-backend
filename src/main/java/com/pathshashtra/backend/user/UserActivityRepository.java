package com.pathshashtra.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    boolean existsByUserIdAndActivityDate(Long userId, LocalDate date);

    /** Returns all activity dates in desc order for streak calculation. */
    @Query("SELECT a.activityDate FROM UserActivity a WHERE a.user.id = :userId ORDER BY a.activityDate DESC")
    List<LocalDate> findDatesByUserIdDesc(Long userId);

    void deleteByUserId(Long userId);
}
