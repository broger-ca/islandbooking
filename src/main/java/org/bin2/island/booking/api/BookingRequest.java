package org.bin2.island.booking.api;

import io.vertx.codegen.annotations.DataObject;
import lombok.Data;

import java.time.LocalDate;

@Data
@DataObject
public class BookingRequest {
    private LocalDate startDate;
    private LocalDate endDate;

    private BookingInfo bookingInfo;
}
