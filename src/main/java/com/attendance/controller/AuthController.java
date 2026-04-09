package com.attendance.controller;

import com.attendance.config.JwtUtil;
import com.attendance.dto.ApiResponse;
import com.attendance.dto.AuthRequest;
import com.attendance.dto.AuthResponse;
import com.attendance.dto.RegisterRequest;
import com.attendance.entity.Role;
import com.attendance.entity.User;
import com.attendance.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    private final UserRepository repo;
    private final JwtUtil jwt;

    public AuthController(UserRepository repo, JwtUtil jwt) {
        this.repo = repo;
        this.jwt = jwt;
    }

    @PostMapping("/signup")
    public String signup(@RequestBody User user) {
        return repo.save(user).getEmail();
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest req) {

        User user = repo.findAll()
                .stream()
                .filter(u -> u.getEmail().equals(req.getEmail()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(req.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwt.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getRole().name());
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest req) {

        // check existing user
        boolean exists = repo.findAll()
                .stream()
                .anyMatch(u -> u.getEmail().equals(req.getEmail()));

        if (exists) {
            return new ApiResponse<>(false, "User already exists", null);
        }

        if (!"ADMIN".equals(req.getRole()) && !"STUDENT".equals(req.getRole())) {
            throw new RuntimeException("Invalid role");
        }

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(req.getPassword()); // ⚠️ later hash this
        user.setRole(
                req.getRole() != null
                        ? Role.valueOf(req.getRole())
                        : Role.STUDENT
        ); // 🔥 role set

        repo.save(user);

        return new ApiResponse<>(true, "User registered successfully", null);
    }
}
