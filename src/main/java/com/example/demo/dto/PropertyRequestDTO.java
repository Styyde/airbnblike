package com.example.demo.dto;

import com.example.demo.Enums.PropertyType;
import com.example.demo.entities.Property;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PropertyRequestDTO {
    @NotBlank @Size(max = 200) private String title;
    @Size(max = 10000) private String description;
    @NotNull private PropertyType type;
    @NotBlank private String address;
    @NotBlank private String city;
    @NotBlank private String country;
    @DecimalMin("-90") @DecimalMax("90") private Double latitude;
    @DecimalMin("-180") @DecimalMax("180") private Double longitude;
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) private BigDecimal pricePerNight;
    @NotNull @Min(1) private Integer maxGuests;
    @NotNull @Min(0) private Integer bedrooms;
    private List<@NotBlank String> amenityIds;
    @Size(max=10) private List<@NotBlank String> imageUrls;

    public Property toEntity() {
        return Property.builder().title(title).description(description).type(type).address(address)
                .city(city).country(country).latitude(latitude).longitude(longitude).pricePerNight(pricePerNight)
                .maxGuests(maxGuests).bedrooms(bedrooms).amenityIds(amenityIds).imageUrls(imageUrls).build();
    }
}
