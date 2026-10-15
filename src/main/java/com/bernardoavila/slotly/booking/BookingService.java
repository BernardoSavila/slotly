package com.bernardoavila.slotly.booking;

import com.bernardoavila.slotly.resource.Resource;
import com.bernardoavila.slotly.resource.ResourceNotFoundException;
import com.bernardoavila.slotly.resource.ResourceRepository;
import com.bernardoavila.slotly.user.User;
import com.bernardoavila.slotly.user.UserNotFoundException;
import com.bernardoavila.slotly.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          ResourceRepository resourceRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public List<BookingResponse> findAll() {
        return bookingRepository.findAll()
                .stream()
                .map(BookingService::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse create(BookingRequest request) {



        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException(request.resourceId()));

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        if (bookingRepository.existsOverlapping(resource.getId(), request.startsAt(), request.endsAt())) {
            throw new OverlappingBookingException(resource.getId(), request.startsAt(), request.endsAt());
        }

        Booking booking = new Booking(resource, user, request.startsAt(), request.endsAt());
        return toResponse(bookingRepository.save(booking));
    }

    private static BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getResource().getId(),
                booking.getResource().getName(),
                booking.getUser().getId(),
                booking.getUser().getName(),
                booking.getStartsAt(),
                booking.getEndsAt()
        );
    }
}