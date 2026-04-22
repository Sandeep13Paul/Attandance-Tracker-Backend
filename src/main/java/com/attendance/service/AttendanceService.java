package com.attendance.service;

import com.attendance.config.JwtUtil;
import com.attendance.dto.ApiResponse;
import com.attendance.dto.AttendanceDTO;
import com.attendance.dto.SubjectNeededDTO;
import com.attendance.entity.Attendance;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.SubjectRepository;
import com.attendance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import com.attendance.utility.DateHelper;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final UserRepository userRepo;
    private final SubjectRepository subjectRepo;

    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public AttendanceService(
            AttendanceRepository attendanceRepo,
            UserRepository userRepo, SubjectRepository subjectRepo, JwtUtil jwtUtil,
            EmailService emailService) {
        this.attendanceRepo = attendanceRepo;
        this.userRepo = userRepo;
        this.subjectRepo = subjectRepo;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
    }

    private User resolveUser(String authHeader, Long userId) {

        String email = jwtUtil.extractEmail(authHeader);
        System.out.println("EMAIL FROM TOKEN: " + email);
        User loggedIn = userRepo.findByEmail(email).orElseThrow();

        // ✅ Admin can query others
        if (userId != null && "ADMIN".equals(loggedIn.getRole())) {
            return userRepo.findById(userId).orElseThrow();
        }

        // ✅ Student always gets own data
        return loggedIn;
    }

    public ApiResponse<AttendanceDTO> mark(
            String authHeader,
            Long userId,      // only used by ADMIN
            Long subjectId,
            boolean present,
            LocalDate date // 🔥 NEW
    ) {

        // 🔐 Extract token
        String email = jwtUtil.extractEmail(authHeader);
        String role = jwtUtil.extractRole(authHeader);

        // 🔍 Logged-in user
        User loggedInUser = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🎯 Decide target user
        User targetUser;

        if ("ADMIN".equals(role)) {
            // Admin can mark for anyone
            targetUser = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Target user not found"));
        } else {
            // Student can only mark their own
            targetUser = loggedInUser;
        }

        // 📘 Get subject
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        // ✅ Prevent duplicate
        attendanceRepo.findByUserAndSubjectAndDate(targetUser, subject, targetDate)
                .ifPresent(attendanceRepo::delete);

        // 📝 Create attendance
        Attendance attendance = new Attendance();
        attendance.setUser(targetUser);
        attendance.setSubject(subject);
        attendance.setDate(targetDate);
        attendance.setPresent(present);

        attendanceRepo.save(attendance);

        // 📦 DTO
        AttendanceDTO dto = new AttendanceDTO(
                targetUser.getId(),
                targetUser.getName(),
                targetDate,
                present
        );

        dto.setSubjectName(subject.getName());

        messagingTemplate.convertAndSend("/topic/attendance", "updated");

        return new ApiResponse<>(true, "Marked", dto);
    }

    public ApiResponse<Page<AttendanceDTO>> getFiltered(
            String authHeader,
            String start,
            String end,
            int page,
            int size
    ) {

        // 🔐 Extract user from JWT
        String email = jwtUtil.extractEmail(authHeader);

        User user = userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        Page<AttendanceDTO> result = attendanceRepo
                .findByUserAndDateBetween(
                        user,
                        LocalDate.parse(start),
                        LocalDate.parse(end),
                        pageable
                )
                .map(a -> {
                    AttendanceDTO dto = new AttendanceDTO(
                            a.getUser().getId(),
                            a.getUser().getName(),
                            a.getDate(),
                            a.isPresent()
                    );

                    dto.setSubjectName(a.getSubject().getName());

                    return dto;
                });

        return new ApiResponse<>(true, "Fetched", result);
    }

    public ApiResponse<String> markAllSubjects(String authHeader, Long userId, boolean present) {

        String email = jwtUtil.extractEmail(authHeader);

        User user = userRepo.findByEmail(email).orElseThrow();
        LocalDate today = LocalDate.now();

        for (Subject subject : subjectRepo.findAll()) {

            attendanceRepo.findByUserAndSubjectAndDate(user, subject, today)
                    .ifPresent(a -> attendanceRepo.delete((Attendance) a));

            Attendance attendance = new Attendance();
            attendance.setUser(user);
            attendance.setSubject(subject);
            attendance.setDate(today);
            attendance.setPresent(present);

            attendanceRepo.save(attendance);
        }

        return new ApiResponse<>(true, "All subjects marked", null);
    }

    public ApiResponse<List<Map<String, Object>>> getSubjectStats(
            String authHeader,
            Long userId // optional
    ) {

        String email = jwtUtil.extractEmail(authHeader);
        String role = jwtUtil.extractRole(authHeader);

        User loggedInUser = userRepo.findByEmail(email).orElseThrow();

        User targetUser;

        if ("ADMIN".equals(role)) {
            targetUser = (userId != null)
                    ? userRepo.findById(userId).orElseThrow()
                    : loggedInUser; // default
        } else {
            targetUser = loggedInUser;
        }

        List<Map<String, Object>> list = new ArrayList<>();

        for (Object[] row : attendanceRepo.getSubjectStatsByUser(targetUser)) {

            String subject = (String) row[0];
            Long present = (Long) row[1];
            Long total = (Long) row[2];

            double percent = total == 0 ? 0 : (present * 100.0 / total);

            list.add(Map.of(
                    "subject", subject,
                    "percent", percent
            ));
        }

        return new ApiResponse<>(true, "Subject stats", list);
    }

    public List<Map<String, Object>> getWeeklyTrend(String authHeader, Long userId, LocalDate start, LocalDate end) {

        User user = resolveUser(authHeader, userId);
        userId = user.getId();

        List<Object[]> rows = attendanceRepo.getWeeklyTrend(userId, start, end);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] r : rows) {
            String week = (String) r[0];
            long present = ((Number) r[1]).longValue();
            long total = ((Number) r[2]).longValue();

            double percent = total == 0 ? 0 : (present * 100.0 / total);

            result.add(Map.of(
                    "week", DateHelper.formatWeek(week),
                    "percent", percent
            ));
        }

        return result;
    }

    public int getStreak(String authHeader, Long userId) {

        User user = resolveUser(authHeader, userId);
        userId = user.getId();

        List<Attendance> list = attendanceRepo
                .findByUserIdOrderByDateDesc(userId);

        if (list.isEmpty()) return 0;

        // Convert to map for quick lookup
        Map<LocalDate, Boolean> attendanceMap = new HashMap<>();

        for (Attendance a : list) {
            attendanceMap.put(a.getDate(), a.isPresent());
        }

        int streak = 0;

        LocalDate current = LocalDate.now();

        if (!attendanceMap.containsKey(current)) {
            current = current.minusDays(1);
        }

        while (true) {

            // ⛔ Skip weekends
            if (DateHelper.isWeekend(current)) {
                current = current.minusDays(1);
                continue;
            }

            Boolean present = attendanceMap.get(current);

            // ❌ No record OR absent → break
            if (present == null || !present) {
                break;
            }

            streak++;
            current = current.minusDays(1);
        }

        return streak;
    }

    public List<Map<String, Object>> getLowAttendance(String authHeader ,Long userId) {

        User user = resolveUser(authHeader, userId);
        userId = user.getId();

        List<Map<String, Object>> list = new ArrayList<>();

        for (Object[] o : attendanceRepo.getLowAttendance(userId)) {
            String subject = (String) o[0];
            Long present = (Long) o[1];
            Long total = (Long) o[2];

            double percent = (present * 100.0 / total);

            list.add(Map.of(
                    "subject", subject,
                    "percent", percent
            ));
        }

        return list;
    }

    public void checkAndSendAlerts(String authHeader, Long userId) {
        User user = userRepo.findById(userId).orElseThrow();

        List<Map<String, Object>> low = getLowAttendance(authHeader, userId);

        for (Map<String, Object> s : low) {
            emailService.sendLowAttendanceAlert(
                    user.getEmail(),
                    (String) s.get("subject"),
                    (Double) s.get("percent")
            );
        }
    }

    @Scheduled(cron = "0 0 9 * * ?") // daily 9 AM
    public void sendDailyAlerts() {
        for (User user : userRepo.findAll()) {
            checkAndSendAlerts(null, user.getId());
        }
    }

    public List<SubjectNeededDTO> getNeededPerSubject(String authHeader, Long userId) {

        User user = resolveUser(authHeader, userId);

        List<Object[]> stats = attendanceRepo.getSubjectStatsByUser(user);

        List<SubjectNeededDTO> result = new ArrayList<>();

        for (Object[] row : stats) {

            String subject = (String) row[0];
            long present = (long) row[1];
            long total = (long) row[2];

            double percent = total == 0 ? 0 : (present * 100.0 / total);

            int needed = 0;

            if (percent < 75) {
                needed = (int) Math.ceil((75 * total - present * 100.0) / (100 - 75));
            }

            result.add(new SubjectNeededDTO(subject, percent, needed));
        }

        return result;
    }
}
