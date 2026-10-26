package com.example.demo.controllers;

import com.example.demo.dto.AvailabilityRequestDTO;
import com.example.demo.entities.Availability;
import com.example.demo.services.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availabilities")
@RequiredArgsConstructor
public class AvailabilityController {
    private final AvailabilityService service;

    @PutMapping
    public Availability update(@Valid @RequestBody AvailabilityRequestDTO request) { return service.update(request); }

    @GetMapping("/{propertyId}")
    public List<Availability> calendar(@PathVariable String propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.getCalendar(propertyId, startDate, endDate);
    }
}
