package com.example.library.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final ReservationService reservationService;

    public NotificationScheduler(NotificationService notificationService, ReservationService reservationService) {
        this.notificationService = notificationService;
        this.reservationService = reservationService;
    }

    @Scheduled(fixedDelayString = "${app.notification.scan-delay-ms:900000}")
    public void scanCirculationEvents() {
        reservationService.expireNotifiedReservations();
        notificationService.generateBorrowingNotifications();
    }
}
