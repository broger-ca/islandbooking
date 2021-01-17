package org.bin2.island.booking.api;

import io.vertx.codegen.annotations.DataObject;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Builder
@DataObject
@Data
public class ErrorsReponse {
    @Singular
    private final List<Error> errors;
}
