package org.bin2.island.booking.api;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import io.micronaut.http.exceptions.HttpException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

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

    @Order(4)
    @Test
    public void testGetDateBookAndCancel() throws Exception {
        LocalDate from = LocalDate.now().plus(3, ChronoUnit.DAYS);
        LocalDate to = from.plus(2, ChronoUnit.DAYS);
        // we check that the dates are available
        checkThatDatesAreAvailable(from, to, 2);
        // we do the booking
        String bookingId = doBooking(from, to);
        // we check that the dates are not available
        checkThatDatesAreAvailable(from, to, 0);
        // trying to rebook the date should make Conflict
        HttpClientResponseException e = Assertions.assertThrows(HttpClientResponseException.class ,()->doBooking(from, to));
        Assertions.assertEquals(HttpStatus.CONFLICT, e.getStatus());
        // we do cancel the booking
        cancelBooking(bookingId);
        // we check that the dates are available
        checkThatDatesAreAvailable(from, to, 2);
    }

    private void cancelBooking(String bookingId) {
        URI uri = UriBuilder.of("/api/v1/booking/").path(bookingId).build();
        client.toBlocking().exchange(HttpRequest.DELETE(uri.toString()));
    }

    private String doBooking(LocalDate from, LocalDate to) {
        URI uri = UriBuilder.of("/api/v1/booking/").build();
        BookingRequest request = BookingRequest.builder()
                .startDate(from)
                .endDate(to)
                .bookingInfo(BookingInfo.builder()
                        .email("testemail@.com")
                        .firstname("firstname")
                        .lastname("lastname")
                        .build())
                .build();
        return client.toBlocking().retrieve(HttpRequest.POST(uri, request));
    }

    private void checkThatDatesAreAvailable(LocalDate from, LocalDate to, int nbDays) throws com.fasterxml.jackson.core.JsonProcessingException {
        URI uri = UriBuilder.of("/api/v1/booking/available").queryParam("from", from).queryParam("to", to).build();
        String response = client.toBlocking().retrieve(HttpRequest.GET(uri));
        List<String> dates = new ObjectMapper().readValue(response, List.class);
        Assertions.assertEquals(nbDays, dates.size());
    }

    @Test
    @Order(5)
    public void testConcurrentBooking() throws Exception {
        LocalDate from = LocalDate.now().plus(6, ChronoUnit.DAYS);
        LocalDate to = from.plus(2, ChronoUnit.DAYS);
        // we check that the dates are available
        checkThatDatesAreAvailable(from, to, 2);
        int nbCalls = 20;
        ExecutorService executor = Executors.newFixedThreadPool(nbCalls);
        var results = Flowable.range(0, nbCalls).parallel(nbCalls).runOn(Schedulers.from(executor))
                .map(i -> {
                    try {
                        System.out.println(Thread.currentThread().getName());
                        return doBooking(from, to);
                    } catch (HttpClientResponseException e) {
                        if (e.getStatus() == HttpStatus.CONFLICT) {
                            return "conflict";
                        }
                        throw e;
                    }
                }).toSortedList(Comparator.naturalOrder()).blockingFirst();
        executor.shutdown();
        //        .map()
        Assertions.assertEquals(nbCalls-1, results.stream().filter(s -> "conflict".equals(s)).count());
        Assertions.assertEquals(1, results.stream().filter(s -> !"conflict".equals(s)).count());
    }

}
