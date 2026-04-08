package com.attendance.controller;

import com.attendance.entity.Subject;
import com.attendance.repository.SubjectRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin
public class SubjectController {

    private final SubjectRepository repo;

    public SubjectController(SubjectRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public Subject add(@RequestBody Subject subject) {
        return repo.save(subject);
    }

    @GetMapping
    public List<Subject> getAll() {
        return repo.findAll();
    }
}
