package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewDTO {
    private String id;
    private String authorId;
    private String authorName; // Pour afficher "Ahmed a dit..." au lieu d'un ID
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
