package com.example.demo.controllers;
import com.example.demo.Enums.PropertyType;
import com.example.demo.dto.PropertyView;
import com.example.demo.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@RestController @RequestMapping("/api/search") @RequiredArgsConstructor
public class SearchController {
    private final CatalogService catalog;
    private final ViewService views;
    @GetMapping("/properties")
    public List<PropertyView> search(@RequestParam(required=false) String city,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required=false) Integer guests,@RequestParam(required=false) PropertyType type,
            @RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice) {
        return catalog.search(city,startDate,endDate,guests,type,minPrice,maxPrice).stream().map(views::property).toList();
    }
}
