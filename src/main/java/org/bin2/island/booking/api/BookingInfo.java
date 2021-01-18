package org.bin2.island.booking.api;

import io.vertx.codegen.annotations.DataObject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@DataObject
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingInfo {
    private String email;
    private String firstname;
    private String lastname;
}
