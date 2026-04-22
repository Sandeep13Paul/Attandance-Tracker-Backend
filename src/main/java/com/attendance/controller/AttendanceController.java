package com.attendance.controller;

import com.attendance.dto.ApiResponse;
import com.attendance.dto.AttendanceDTO;
import com.attendance.service.AttendanceService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@CrossOrigin(origins = "*")   // 🔥 allow frontend
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping("/mark")
    public ApiResponse<?> mark(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long userId,
            @RequestParam Long subjectId,
            @RequestParam boolean present,
            @RequestParam(required = false) String date
    ) {
        LocalDate d = (date != null) ? LocalDate.parse(date) : null;
        return service.mark(authHeader, userId, subjectId, present, d);
    }

    @PostMapping("/mark-all")
    public ApiResponse<String> markAll(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam Long userId,
            @RequestParam boolean present
    ) {
        return service.markAllSubjects(authHeader, userId, present);
    }

    @GetMapping
    public ApiResponse<?> get(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getFiltered(authHeader, start, end, page, size);
    }

    @GetMapping("/subject-stats")
    public ApiResponse<?> subjectStats(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0", required = false) Long userId
    ) {
        return service.getSubjectStats(authHeader, userId);
    }

    @GetMapping("/trend")
    public ApiResponse<?> trend(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0", required = false) Long userId,
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
    ) {
        return new ApiResponse<>(true, "Trend", service.getWeeklyTrend(authHeader, userId, start, end));
    }

    @GetMapping("/streak")
    public ApiResponse<?> streak(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0", required = false) Long userId
    ) {
        return new ApiResponse<>(true, "Streak", service.getStreak(authHeader, userId));
    }

    @GetMapping("/low")
    public ApiResponse<?> low(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0", required = false) Long userId
    ) {
        return new ApiResponse<>(true, "Low",
                service.getLowAttendance(authHeader, userId));
    }

    @GetMapping("/needed")
    public ApiResponse<?> needed(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0", required = false) Long userId
    ) {
        return new ApiResponse<>(true, "Needed",
                service.getNeededPerSubject(authHeader, userId));
    }
}
