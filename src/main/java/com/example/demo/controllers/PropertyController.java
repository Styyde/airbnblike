package com.example.demo.controllers;

import com.example.demo.annotations.CurrentUser;
import com.example.demo.entities.Property;
import com.example.demo.dto.PropertyRequestDTO;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.services.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public ResponseEntity<List<Property>> getAllProperties() {
        return ResponseEntity.ok(propertyService.getAllProperties());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Property> getPropertyById(@PathVariable String id) {
        var property = propertyService.getPropertyById(id);
        if (property.getStatus() != com.example.demo.Enums.PropertyStatus.AVAILABLE) {
            throw com.example.demo.exceptions.ApiException.notFound("Logement introuvable");
        }
        return ResponseEntity.ok(property);
    }

    @GetMapping("/owner/me")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<Property>> getMyProperties(@CurrentUser CustomUserDetails currentUser) {
        return ResponseEntity.ok(propertyService.getPropertiesByOwner(currentUser.getId()));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasRole('ADMIN') or #ownerId == authentication.principal.id")
    public ResponseEntity<List<Property>> getPropertiesByOwner(@PathVariable String ownerId) {
        return ResponseEntity.ok(propertyService.getPropertiesByOwner(ownerId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Property> createProperty(
            @Valid @RequestBody PropertyRequestDTO request,
            @CurrentUser CustomUserDetails currentUser) {

        Property property = request.toEntity();
        property.setOwnerId(currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(propertyService.createProperty(property));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @propertyService.getPropertyById(#id).ownerId == authentication.principal.id")
    public ResponseEntity<Property> updateProperty(@PathVariable String id, @Valid @RequestBody PropertyRequestDTO request) {
        return ResponseEntity.ok(propertyService.updateProperty(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @propertyService.getPropertyById(#id).ownerId == authentication.principal.id")
    public ResponseEntity<Void> deleteProperty(@PathVariable String id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }
}
