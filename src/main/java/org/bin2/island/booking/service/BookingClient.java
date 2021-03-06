package org.bin2.island.booking.service;

import org.bin2.island.booking.model.BookingAction;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;

@KafkaClient
public interface BookingClient {

    @Topic("booking-update")
    void sendEvent(@KafkaKey String bookingId, BookingAction action);

}
