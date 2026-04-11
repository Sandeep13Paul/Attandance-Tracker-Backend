package com.attendance.service;

import com.attendance.config.JwtUtil;
import com.attendance.dto.ApiResponse;
import com.attendance.dto.AttendanceDTO;
import com.attendance.entity.Attendance;
import com.attendance.entity.Subject;
import com.attendance.entity.User;
import com.attendance.repository.AttendanceRepository;
import com.attendance.repository.SubjectRepository;
import com.attendance.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final UserRepository userRepo;
    private final SubjectRepository subjectRepo;

    private final JwtUtil jwtUtil;

    public AttendanceService(
            AttendanceRepository attendanceRepo,
            UserRepository userRepo, SubjectRepository subjectRepo, JwtUtil jwtUtil
    ) {
        this.attendanceRepo = attendanceRepo;
        this.userRepo = userRepo;
        this.subjectRepo = subjectRepo;
        this.jwtUtil = jwtUtil;
    }

    public ApiResponse<AttendanceDTO> mark(
            String authHeader,
            Long userId,      // only used by ADMIN
            Long subjectId,
            boolean present
    ) {

        // 🔐 Extract token
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);

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

        LocalDate today = LocalDate.now();

        // ✅ Prevent duplicate
        attendanceRepo.findByUserAndSubjectAndDate(targetUser, subject, today)
                .ifPresent(attendanceRepo::delete);

        // 📝 Create attendance
        Attendance attendance = new Attendance();
        attendance.setUser(targetUser);
        attendance.setSubject(subject);
        attendance.setDate(today);
        attendance.setPresent(present);

        attendanceRepo.save(attendance);

        // 📦 DTO
        AttendanceDTO dto = new AttendanceDTO(
                targetUser.getId(),
                targetUser.getName(),
                attendance.getDate(),
                present
        );

        dto.setSubjectName(subject.getName());

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
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);

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

    public ApiResponse<String> markAllSubjects(Long userId, boolean present) {

        User user = userRepo.findById(userId).orElseThrow();
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

        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);

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
}
