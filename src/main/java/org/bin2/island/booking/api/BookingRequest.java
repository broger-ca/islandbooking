package org.bin2.island.booking.api;

import io.vertx.codegen.annotations.DataObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@DataObject
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingRequest {
    private LocalDate startDate;
    private LocalDate endDate;

    private BookingInfo bookingInfo;
}
