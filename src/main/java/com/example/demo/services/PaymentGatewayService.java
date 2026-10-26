package com.example.demo.services;

import com.example.demo.Enums.PaymentMethod;
import com.example.demo.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

/** Explicit demo gateway. Real payments require a separate provider adapter and webhooks. */
@Service
public class PaymentGatewayService {
    private final String mode;

    public PaymentGatewayService(@Value("${app.payment.mode:disabled}") String mode) { this.mode = mode; }

    public void requireEnabled() {
        if (!"demo".equals(mode)) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Paiement désactivé ; activer explicitement le mode demo");
    }

    public String charge(BigDecimal amount, PaymentMethod method, String userId) {
        requireEnabled();
        if (amount == null || amount.signum() <= 0 || method == null) throw ApiException.badRequest("Paiement invalide");
        return "demo_" + UUID.randomUUID();
    }

    public void refund(String transactionId, BigDecimal amount) {
        requireEnabled();
        if (transactionId == null || !transactionId.startsWith("demo_")) throw ApiException.conflict("Transaction de démonstration introuvable");
    }
}
