package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BookingRequestDTO {
    @jakarta.validation.constraints.NotBlank
    private String propertyId;
    private String userId;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.FutureOrPresent
    private LocalDate startDate;
    @jakarta.validation.constraints.NotNull
    private LocalDate endDate;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Min(1)
    private Integer numberOfGuests;
}
