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

    Optional<Attendance> findByUserAndSubjectAndDate(
            User user,
            Subject subject,
            LocalDate date
    );

    @Query("""
    SELECT s.name,\s
           SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END),\s
           COUNT(a)
    FROM Attendance a
    JOIN a.subject s
    GROUP BY s.name
   \s""")
    List<Object[]> getSubjectStats();

    Page<Attendance> findByUserAndDateBetween(
            User user,
            LocalDate start,
            LocalDate end,
            Pageable pageable
    );

    @Query("""
    SELECT s.name,
           SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END),
           COUNT(a)
    FROM Attendance a
    JOIN a.subject s
    WHERE a.user = :user
    GROUP BY s.name
""")
    List<Object[]> getSubjectStatsByUser(User user);

    @Query(value = """
SELECT TO_CHAR(a.date, 'IYYY-IW') AS week,
       SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END) AS present,
       COUNT(a.id) AS total
FROM attendance a
WHERE (:userId IS NULL OR a.user_id = :userId)
AND a.date BETWEEN :start AND :end
GROUP BY week
ORDER BY week
""", nativeQuery = true)
    List<Object[]> getWeeklyTrend(Long userId, LocalDate start, LocalDate end);

    @Query("""
SELECT a.date, a.present
FROM Attendance a
WHERE a.user.id = :userId
""")
    List<Object[]> getHeatmap(Long userId);

    List<Attendance> findByUserIdOrderByDateDesc(Long userId);

    @Query("""
SELECT s.name,
SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END),
COUNT(a)
FROM Attendance a
JOIN a.subject s
WHERE a.user.id = :userId
GROUP BY s.name
HAVING (SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END) * 100.0 / COUNT(a)) < 75
""")
    List<Object[]> getLowAttendance(Long userId);

    List<Attendance> findByUserAndDateBetween(
            User user,
            LocalDate start,
            LocalDate end
    );
}