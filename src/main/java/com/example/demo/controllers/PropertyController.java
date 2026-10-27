package com.example.demo.controllers;

import com.example.demo.annotations.CurrentUser;
import com.example.demo.dto.*;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.services.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {
    private final PropertyService service;
    private final CatalogService catalog;
    private final ViewService views;
    @GetMapping public List<PropertyView> all() { return service.getAllProperties().stream().map(views::property).toList(); }
    @GetMapping("/{id}") public PropertyView one(@PathVariable String id) { return views.property(catalog.published(id)); }
    @GetMapping("/{id}/calendar")
    public List<CatalogService.Day> calendar(@PathVariable String id,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate) { return catalog.calendar(id,startDate,endDate); }
    @GetMapping("/{id}/quote")
    public CatalogService.Quote quote(@PathVariable String id,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam int guests) { return catalog.quote(id,startDate,endDate,guests); }
    @GetMapping("/owner/me") @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<PropertyView> mine(@CurrentUser CustomUserDetails user) { return service.getPropertiesByOwner(user.getId()).stream().map(views::property).toList(); }
    @GetMapping("/owner/{ownerId}") @PreAuthorize("hasRole('ADMIN') or #ownerId == authentication.principal.id")
    public List<PropertyView> owner(@PathVariable String ownerId) { return service.getPropertiesByOwner(ownerId).stream().map(views::property).toList(); }
    @PostMapping @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<PropertyView> create(@Valid @RequestBody PropertyRequestDTO request, @CurrentUser CustomUserDetails user) {
        var p=request.toEntity(); p.setOwnerId(user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(views.property(service.createProperty(p)));
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN') or @propertyService.getPropertyById(#id).ownerId == authentication.principal.id")
    public PropertyView update(@PathVariable String id,@Valid @RequestBody PropertyRequestDTO request) { return views.property(service.updateProperty(id,request.toEntity())); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN') or @propertyService.getPropertyById(#id).ownerId == authentication.principal.id")
    public ResponseEntity<Void> delete(@PathVariable String id) { service.deleteProperty(id); return ResponseEntity.noContent().build(); }
}
