package org.bin2.island.booking.model;

import io.vertx.codegen.annotations.DataObject;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@DataObject
@Data
@Builder(toBuilder = true)
public class BookingDate {

    private LocalDate date;
    private String bookingId;
}
