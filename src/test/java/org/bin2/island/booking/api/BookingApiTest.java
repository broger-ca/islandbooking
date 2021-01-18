package org.bin2.island.booking.api;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

public class BookingApiTest extends BaseDbContainerTest {
    @Inject
    EmbeddedServer server;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    @Order(1)
    public void testGetDateAvailableNotArgs() throws Exception {
        String response = client.toBlocking().retrieve("/api/v1/booking/available");
        List<String> dates = new ObjectMapper().readValue(response, List.class);
        int nbDays = (int)LocalDate.now().until(LocalDate.now().plus(1, ChronoUnit.MONTHS), ChronoUnit.DAYS);
        Assertions.assertEquals(nbDays , dates.size(), "number of expected dates does not match");
        // first day is tomorrow and the last day is today + one month
        var expected = LocalDate.now().plus(1, ChronoUnit.DAYS)
                .datesUntil(LocalDate.now().plus(1, ChronoUnit.MONTHS).plus(1, ChronoUnit.DAYS))
                .map(d -> d.toString()).collect(Collectors.toList());
        Assertions.assertEquals(expected ,dates);
    }

    @Test
    @Order(2)
    public void testGetDateAvailableValidDateRange() throws Exception {
        int nbDays = 5;
        LocalDate from = LocalDate.now().plus(4, ChronoUnit.DAYS);
        LocalDate to = from.plus(nbDays, ChronoUnit.DAYS);
        URI uri = UriBuilder.of("/api/v1/booking/available").queryParam("from", from).queryParam("to", to).build();

        String response = client.toBlocking().retrieve(HttpRequest.GET(uri));
        List<String> dates = new ObjectMapper().readValue(response, List.class);
        Assertions.assertEquals(nbDays , dates.size(), "number of expected dates does not match");
        var expected = from
                .datesUntil(to)
                .map(d -> d.toString()).collect(Collectors.toList());
        Assertions.assertEquals(expected ,dates);
    }

    @Order(3)
    @Test
    public void testGetDateAvailableInValidDateRange() throws Exception {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plus(10, ChronoUnit.DAYS);
        URI uri = UriBuilder.of("/api/v1/booking/available").queryParam("from", from).queryParam("to", to).build();
        var e = Assertions.assertThrows(HttpClientResponseException.class, ()-> client.toBlocking().retrieve(HttpRequest.GET(uri)));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }
}
