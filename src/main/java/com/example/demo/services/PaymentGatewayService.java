package com.example.demo.services;

import com.example.demo.Enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class PaymentGatewayService {

    /**
     * Simuler un appel à Stripe/PayPal
     */
    public String charge(Double amount, PaymentMethod method, String userId) {
        log.info("Processing payment of {} via {} for user {}", amount, method, userId);

        // Simulation : 5% de chance d'échec
        if (Math.random() < 0.05) {
            throw new RuntimeException("Transaction declined by bank");
        }

        return "txn_" + UUID.randomUUID().toString();
    }

    /**
     * Remboursement
     */
    public void refund(String paymentId, Double amount) {
        log.info("Refunding {} for payment {}", amount, paymentId);

        if (Math.random() < 0.02) {
            throw new RuntimeException("Refund failed: insufficient balance");
        }

        log.info("Refund successful for payment {}", paymentId);
    }
}