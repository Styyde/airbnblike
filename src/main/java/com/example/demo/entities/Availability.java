package com.example.demo.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;

@Document(collection = "availabilities")
@org.springframework.data.mongodb.core.index.CompoundIndex(name = "property_date_unique", def = "{'propertyId':1,'date':1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {
    @Id
    private String id;
    private String propertyId;
    private LocalDate date;
    private Boolean isAvailable;
    @org.springframework.data.mongodb.core.mapping.Field(targetType = org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128)
    private java.math.BigDecimal priceOverride;
}
