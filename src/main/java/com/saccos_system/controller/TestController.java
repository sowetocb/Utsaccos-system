package com.saccos_system.controller;


import com.saccos_system.model.StaffProfile;
import com.saccos_system.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final StaffProfileRepository staffProfileRepository;

    @GetMapping("/connection")
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok("Database connection is working!");
    }

    @GetMapping("/members")
    public ResponseEntity<List<StaffProfile>> getAllMembers() {
        return ResponseEntity.ok(staffProfileRepository.findAll());
    }

    @PostMapping("/members")
    public ResponseEntity<StaffProfile> createMember(@RequestBody StaffProfile member) {
        return ResponseEntity.ok(staffProfileRepository.save(member));
    }
}