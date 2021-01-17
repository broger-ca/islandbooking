package org.bin2.island.booking.service;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaListener(offsetReset =  OffsetReset.LATEST)
public class BookingEventListener {
    private final BookingService bookingService;

    public BookingEventListener(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Topic("booking-update")
    public void receive(@KafkaKey String bookingId) {
        bookingService.refreshCache();
    }
}
