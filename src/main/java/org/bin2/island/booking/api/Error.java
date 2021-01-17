package org.bin2.island.booking.api;

import io.vertx.codegen.annotations.DataObject;
import lombok.Builder;
import lombok.Data;

@DataObject
@Data
@Builder
public class Error {
    private String code;
    private String message;
}
