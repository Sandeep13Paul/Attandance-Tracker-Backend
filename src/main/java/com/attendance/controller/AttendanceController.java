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
            @RequestParam Long userId,
            @RequestParam boolean present
    ) {
        return service.markAllSubjects(userId, present);
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
            @RequestParam(required = false) Long userId
    ) {
        return service.getSubjectStats(authHeader, userId);
    }
}
