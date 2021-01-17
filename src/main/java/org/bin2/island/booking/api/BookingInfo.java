package org.bin2.island.booking.api;

import io.vertx.codegen.annotations.DataObject;
import lombok.Data;

@DataObject
@Data
public class BookingInfo {
    private String email;
    private String firstname;
    private String lastname;
}
