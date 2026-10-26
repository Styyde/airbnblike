package com.example.demo.controllers;

import com.example.demo.entities.User;
import com.example.demo.services.UserService;
import com.example.demo.services.PropertyService;
import com.example.demo.services.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final PropertyService propertyService;
    private final BookingService bookingService;

    @GetMapping("/users")
    public ResponseEntity<List<com.example.demo.dto.UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers().stream().map(userService::toDTO).toList());
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<com.example.demo.dto.UserResponseDTO> assignRole(@PathVariable String userId, @RequestParam String role) {
        return ResponseEntity.ok(userService.toDTO(userService.assignRole(userId, role)));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.countUsers());
        stats.put("totalProperties", propertyService.countAllProperties());
        stats.put("totalBookings", bookingService.countAllBookings());
        stats.put("totalRevenue", bookingService.getTotalRevenue());
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/properties/{propertyId}/status")
    public ResponseEntity<com.example.demo.entities.Property> updatePropertyStatus(
            @PathVariable String propertyId, @RequestParam com.example.demo.Enums.PropertyStatus status) {
        return ResponseEntity.ok(propertyService.updateStatus(propertyId, status));
    }
}
