package com.example.demo.controllers;

import com.example.demo.Enums.PaymentMethod;
import com.example.demo.entities.Payment;
import com.example.demo.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process/{bookingId}")
    public ResponseEntity<Payment> processPayment(
            @PathVariable String bookingId,
            @RequestParam PaymentMethod method) {
        return ResponseEntity.ok(paymentService.processPayment(bookingId, method));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Payment> getPaymentByBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Payment> refundPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }
}