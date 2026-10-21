package com.bernardoavila.slotly.booking;

import com.bernardoavila.slotly.PostgresTestBase;
import com.bernardoavila.slotly.resource.Resource;
import com.bernardoavila.slotly.resource.ResourceRepository;
import com.bernardoavila.slotly.user.User;
import com.bernardoavila.slotly.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BookingConcurrencyTest extends PostgresTestBase {

    private static final int THREADS = 10;
    private static final Instant NINE = Instant.parse("2026-11-20T09:00:00Z");
    private static final Instant TEN = Instant.parse("2026-11-20T10:00:00Z");

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private UserRepository userRepository;

    private Long resourceId;
    private Long userId;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        Resource room = resourceRepository.save(new Resource("Sala Concorrida", null));
        User user = userRepository.save(new User("concurrent@example.com", "Teste", "not-a-real-hash"));
        resourceId = room.getId();
        userId = user.getId();
    }

    @Test
    @DisplayName("only one of many concurrent requests for the same slot succeeds")
    void onlyOneBookingSurvivesConcurrentRequests() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger created = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    bookingService.create(new BookingRequest(resourceId, userId, NINE, TEN));
                    created.incrementAndGet();
                } catch (OverlappingBookingException e) {
                    rejected.incrementAndGet();
                } catch (Exception e) {
                    rejected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(created.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(THREADS - 1);
        assertThat(bookingRepository.findAll()).hasSize(1);
    }
}