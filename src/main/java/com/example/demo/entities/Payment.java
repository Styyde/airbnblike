package com.example.demo.entities;

import com.example.demo.Enums.PaymentMethod;
import com.example.demo.Enums.PaymentStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    private String id;
    private Double amount;
    private PaymentMethod method;  // ← Changé de String à PaymentMethod
    private PaymentStatus status;   // ← Changé de String à PaymentStatus
    private String bookingId;

    @Builder.Default
    private LocalDateTime paymentDate = LocalDateTime.now();
}