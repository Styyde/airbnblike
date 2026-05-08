package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequestDTO {
    private String propertyId;
    private String userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer numberOfGuests;
}