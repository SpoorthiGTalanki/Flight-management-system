package com.example.flight.service;

import com.example.flight.dto.NotificationRequestDTO;
import com.example.flight.dto.NotificationResponseDTO;
import com.example.flight.entity.Booking;
import com.example.flight.entity.Notification;
import com.example.flight.entity.User;
import com.example.flight.repository.BookingRepository;
import com.example.flight.repository.NotificationRepository;
import com.example.flight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;

    // Get all notifications
    public List<NotificationResponseDTO> getAllNotifications() {

        return notificationRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get notification by ID
    public NotificationResponseDTO getNotificationById(
            Long notificationId) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found with ID: "
                                                + notificationId
                                ));

        return convertToResponse(notification);
    }

    // Get notifications by user
    public List<NotificationResponseDTO> getNotificationsByUser(
            Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new RuntimeException(
                    "User not found with ID: " + userId
            );
        }

        return notificationRepository
                .findByUserUserId(userId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get notifications by booking
    public List<NotificationResponseDTO> getNotificationsByBooking(
            Long bookingId) {

        if (!bookingRepository.existsById(bookingId)) {
            throw new RuntimeException(
                    "Booking not found with ID: " + bookingId
            );
        }

        return notificationRepository
                .findByBookingBookingId(bookingId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get notifications by status
    public List<NotificationResponseDTO> getNotificationsByStatus(
            String status) {

        return notificationRepository
                .findByStatus(status)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Create notification
    public NotificationResponseDTO createNotification(
            NotificationRequestDTO dto) {

        User user =
                userRepository.findById(dto.getUserId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found with ID: "
                                                + dto.getUserId()
                                ));

        Notification notification =
                modelMapper.map(
                        dto,
                        Notification.class
                );

        // Set User relationship
        notification.setUser(user);

        // Booking is optional
        if (dto.getBookingId() != null) {

            Booking booking =
                    bookingRepository.findById(
                            dto.getBookingId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Booking not found with ID: "
                                            + dto.getBookingId()
                            ));

            notification.setBooking(booking);
        }

        // Default status
        if (dto.getStatus() == null ||
                dto.getStatus().isBlank()) {

            notification.setStatus("pending");
        }

        notification.setCreatedAt(
                LocalDateTime.now()
        );

        Notification savedNotification =
                notificationRepository.save(notification);

        return convertToResponse(savedNotification);
    }

    // Update notification
    public NotificationResponseDTO updateNotification(
            Long notificationId,
            NotificationRequestDTO dto) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found with ID: "
                                                + notificationId
                                ));

        User user =
                userRepository.findById(dto.getUserId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found with ID: "
                                                + dto.getUserId()
                                ));

        modelMapper.map(dto, notification);

        notification.setUser(user);

        // Booking is optional
        if (dto.getBookingId() != null) {

            Booking booking =
                    bookingRepository.findById(
                            dto.getBookingId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Booking not found with ID: "
                                            + dto.getBookingId()
                            ));

            notification.setBooking(booking);

        } else {

            notification.setBooking(null);
        }

        Notification updatedNotification =
                notificationRepository.save(notification);

        return convertToResponse(updatedNotification);
    }

    // Delete notification
    public void deleteNotification(Long notificationId) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found with ID: "
                                                + notificationId
                                ));

        notificationRepository.delete(notification);
    }

    // Entity -> Response DTO
    private NotificationResponseDTO convertToResponse(
            Notification notification) {

        NotificationResponseDTO response =
                modelMapper.map(
                        notification,
                        NotificationResponseDTO.class
                );

        response.setUserId(
                notification.getUser().getUserId()
        );

        if (notification.getBooking() != null) {

            response.setBookingId(
                    notification.getBooking().getBookingId()
            );
        }

        return response;
    }
}