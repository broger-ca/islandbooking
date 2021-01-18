package org.bin2.island.booking.api;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest(rebuildContext = true)
public class BookingApiTest extends BaseDbContainerTest {
    @Inject
    EmbeddedServer server;

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    public void testGetDateAvailableNotArgs() throws Exception {
        String response = client.toBlocking().retrieve("/api/v1/booking/available");
        List<String> dates = new ObjectMapper().readValue(response, List.class);
        int nbDays = (int)LocalDate.now().until(LocalDate.now().plus(1, ChronoUnit.MONTHS), ChronoUnit.DAYS);
        Assertions.assertEquals(nbDays , dates.size(), "number of expected dates does not match");
    }
}
