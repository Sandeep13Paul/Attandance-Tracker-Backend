package com.attendance.repository;

import com.attendance.entity.Attendance;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Page<Attendance> findByDateBetween(
            LocalDate start,
            LocalDate end,
            Pageable pageable
    );

    Optional<Object> findByUserAndSubjectAndDate(User user, Subject subject, LocalDate today);

    @Query("""
    SELECT s.name,\s
           SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END),\s
           COUNT(a)
    FROM Attendance a
    JOIN a.subject s
    GROUP BY s.name
   \s""")
    List<Object[]> getSubjectStats();
}