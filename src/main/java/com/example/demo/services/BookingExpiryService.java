package com.example.demo.services;

import com.example.demo.Enums.BookingStatus;
import com.example.demo.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.booking.expiry-enabled", havingValue = "true", matchIfMissing = true)
public class BookingExpiryService {
    private final BookingRepository repository;
    private final BookingService service;

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void expire() {
        for (var booking : repository.findByStatusInAndExpiresAtBefore(java.util.List.of(BookingStatus.REQUESTED, BookingStatus.PENDING), java.time.Instant.now())) {
            try { service.expireBooking(booking.getId()); }
            catch (org.springframework.dao.TransientDataAccessException ex) {
                log.debug("Expiration concurrente de {} ; nouvel essai au prochain passage", booking.getId());
            }
        }
    }
}
