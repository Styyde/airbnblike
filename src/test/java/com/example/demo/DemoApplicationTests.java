package com.example.demo;

import com.example.demo.dto.*;
import com.example.demo.services.StayDates;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class DemoApplicationTests {
    @Test void diagnosticsDoNotExposePasswordsOrTokens() {
        var registration = RegisterRequestDTO.builder().password("sensitive-password").confirmPassword("sensitive-confirmation").build();
        var login = LoginRequestDTO.builder().password("sensitive-password").build();
        var response = LoginResponseDTO.builder().token("sensitive-token").build();
        assertThat(registration.toString()).doesNotContain("sensitive-password", "sensitive-confirmation");
        assertThat(login.toString()).doesNotContain("sensitive-password");
        assertThat(response.toString()).doesNotContain("sensitive-token");
    }

    @Test void paymentSimulationRequiresExplicitActivation() {
        var gateway = new com.example.demo.services.PaymentGatewayService("disabled");
        assertThatThrownBy(gateway::requireEnabled).isInstanceOf(com.example.demo.exceptions.ApiException.class)
                .extracting("status").isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void stayRequiresFutureOrderedDatesAndBoundedDuration() {
        var today = LocalDate.now();
        assertThatCode(() -> StayDates.validate(today, today.plusDays(1))).doesNotThrowAnyException();
        assertThatThrownBy(() -> StayDates.validate(today, today)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> StayDates.validate(today.minusDays(1), today)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> StayDates.validate(today, today.plusYears(2))).isInstanceOf(RuntimeException.class);
    }

    @Test
    void registrationChecksPasswordConfirmationAndBookingRequiresFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = RegisterRequestDTO.builder().username("alice").email("alice@example.org")
                    .password("password123").confirmPassword("different").build();
            assertThat(validator.validate(request)).anyMatch(v -> v.getPropertyPath().toString().equals("passwordConfirmed"));
            request.setConfirmPassword("password123");
            assertThat(validator.validate(request)).isEmpty();
            assertThat(validator.validate(new BookingRequestDTO())).hasSize(4);
        }
    }
}
