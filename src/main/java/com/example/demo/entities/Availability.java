package com.example.demo.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document(collection = "availabilities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {
    @Id
    private String id;
    private String propertyId;
    private LocalDate date;
    private Boolean isAvailable;
    private Double priceOverride; // Pour gérer les prix plus chers le weekend par exemple
}
