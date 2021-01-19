package org.bin2.island.booking.api;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import javax.inject.Singleton;

import com.google.common.collect.Lists;

@Singleton
public class BookingRequestValidator {
    List<Error> validateBookingRequest(BookingRequest request) {
        List<Error> errors = Lists.newArrayList();
        validateMandatoryField(request.getStartDate(), "startDate").ifPresent(errors::add);
        validateMandatoryField(request.getEndDate(), "endDate").ifPresent(errors::add);
        if (request.getBookingInfo()==null) {
            errors.add(Error.builder()
                    .code("MISSING_FIELD")
                    .message("Missing field bookingInfo")
                    .build());
        } else {
            validateMandatoryField(request.getBookingInfo().getEmail(), "bookingInfo.email").ifPresent(errors::add);
            validateMandatoryField(request.getBookingInfo().getFirstname(), "bookingInfo.firstname").ifPresent(errors::add);
            validateMandatoryField(request.getBookingInfo().getLastname(), "bookingInfo.lastname").ifPresent(errors::add);
        }
        if (request.getStartDate()!=null && request.getEndDate()!=null) {
            if (request.getStartDate().isBefore(LocalDate.now().plus(1, ChronoUnit.DAYS)) ||
                    request.getEndDate().isAfter(LocalDate.now().plus(1, ChronoUnit.MONTHS).plus(1, ChronoUnit.DAYS)) ) {
                errors.add(Error.builder()
                                .code("BAD_REQUEST")
                                .message("should book at least one day before and maximum 1 month in advance")
                                .build());
            }
            if (!request.getEndDate().isAfter(request.getStartDate())) {
                errors.add(Error.builder().message("'endDate' should be after 'startDate'").build());
            }

            if (request.getStartDate().until(request.getEndDate(), ChronoUnit.DAYS)>3) {
                errors.add(Error.builder().message("Cannot book more than 3 days").build());
            }
        }
        return errors;
    }

    private <T> Optional<Error> validateMandatoryField(T value, String field) {
        if (value == null) {
            return Optional.of(Error.builder().code("MISSING_FIELD")
                    .message(String.format("Missing mandatory field '%s'", field))
                    .build());
        } else {
            return Optional.empty();
        }
    }
}
