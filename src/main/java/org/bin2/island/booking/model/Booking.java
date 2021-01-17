package org.bin2.island.booking.model;

import io.vertx.codegen.annotations.DataObject;
import lombok.Builder;
import lombok.Data;

@DataObject
@Data
@Builder(toBuilder = true)
public class Booking {

    private final String id;
    private final String firstName;
    private final String lastName;
    private final String email;

}
