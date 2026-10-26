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
    @org.springframework.data.mongodb.core.mapping.Field(targetType = org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128)
    private java.math.BigDecimal amount;
    private PaymentMethod method;  // ← Changé de String à PaymentMethod
    private PaymentStatus status;   // ← Changé de String à PaymentStatus
    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    private String bookingId;
    private String gatewayTransactionId;
    @org.springframework.data.annotation.Version
    private Long version;

    @Builder.Default
    private LocalDateTime paymentDate = LocalDateTime.now();
}
