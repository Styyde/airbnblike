package com.example.demo.controllers;

import com.example.demo.entities.Amenity;
import com.example.demo.repositories.AmenityRepository;
import com.example.demo.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
public class AmenityController {
    private final AmenityRepository repository;
    @GetMapping
    public List<Amenity> all() { return repository.findAll(); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Amenity create(@RequestBody Amenity amenity) {
        if (amenity.getName() == null || amenity.getName().isBlank()) throw ApiException.badRequest("Nom requis");
        amenity.setId(null);
        return repository.save(amenity);
    }
}
