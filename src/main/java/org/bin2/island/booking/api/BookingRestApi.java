package org.bin2.island.booking.api;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import org.bin2.island.booking.model.Booking;
import org.bin2.island.booking.service.BookingService;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/api/v1/booking")
@AllArgsConstructor
public class BookingRestApi {

    private final BookingService bookingService;
    private final BookingRequestValidator bookingRequestValidator;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/available")
    public Single<Response> getAvailableDates(@Nullable  @QueryParam("from") String fromAsString,@Nullable  @QueryParam("to") String toAsString) {
        final LocalDate from;
        final LocalDate to;
        try {
            from = Optional.ofNullable(fromAsString).map(LocalDate::parse).orElse(null);
        } catch (DateTimeParseException e) {
            return Single.just(Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorsReponse.builder()
                            .error(Error.builder().message("Illegal date format for parameter from").build())
                            .build())
                    .build());
        }
        try {
            to = Optional.ofNullable(toAsString).map(LocalDate::parse).orElse(null);
        } catch (DateTimeParseException e) {
            return Single.just(Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorsReponse.builder()
                            .error(Error.builder().message("Illegal date format for parameter to").build())
                            .build())
                    .build());
        }
        if (from !=null && to!=null && !to.isAfter(from)) {
            return Single.just(Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorsReponse.builder()
                            .error(Error.builder().message("'to' should be after 'from'").build())
                            .build())
                    .build());
        }
        LocalDate minDate = LocalDate.now().plus(1, ChronoUnit.DAYS);
        LocalDate maxDate = minDate.plus(1, ChronoUnit.MONTHS);

        if(from != null && (from.isBefore(minDate) || from.isAfter(maxDate) )) {
            return Single.just(Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorsReponse.builder()
                            .error(Error.builder().message(String.format("from date should be between %s and %s", minDate, maxDate)).build())
                            .build())
                    .build());
        } else if (to != null && to.isAfter(maxDate)) {
            return Single.just(Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorsReponse.builder()
                            .error(Error.builder().message(String.format("from date should be lesser or equals than %s", minDate, maxDate)).build())
                            .build())
                    .build());
        } else {
            return bookingService.getAvailableDates(Optional.ofNullable(from).orElse(minDate),
                    Optional.ofNullable(to).orElse(maxDate)
            ).toList()
                    .map(l -> Response.ok(l).build());
        }
    }

    @POST
    public Single<Response> bookDate(BookingRequest request) {
        List<Error> errors = this.bookingRequestValidator.validateBookingRequest(request);
        if (errors!=null&&!errors.isEmpty()) {
            return Single.just(Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorsReponse.builder().errors(errors).build()).build());
        }
        Booking booking = Booking.builder()
                .email(request.getBookingInfo().getEmail())
                .firstName(request.getBookingInfo().getFirstname())
                .lastName(request.getBookingInfo().getLastname())
                .build();
        return bookingService.tryToBook(booking, request.getStartDate(), request.getEndDate())
                .map(b ->
                        Response.ok(b.getId()).build())
                .switchIfEmpty(Single.just(
                        Response.status(Response.Status.CONFLICT)
                                .entity(ErrorsReponse.builder()
                                        .error(Error.builder().code("CONFLICT")
                                                .message(String.format("from %s to %s is conflicting with another booking", request.getStartDate(), request.getEndDate())).build()).build())
                                .build()));
    }

    @PUT
    @Path("/{bookingId}")
    public Single<Response> updateBookingInfo(@PathParam("bookingId") String bookingId, BookingInfo bookingInfo) {
        Booking booking = Booking.builder()
                .email(bookingInfo.getEmail())
                .firstName(bookingInfo.getFirstname())
                .lastName(bookingInfo.getLastname())
                .id(bookingId)
                .build();
        return bookingService.updateBooking(booking).map(b -> Response.status(Response.Status.NO_CONTENT).build())
                .switchIfEmpty(buildNotFoundResponse("booking not found "));
    }

    @DELETE
    @Path("/{bookingId}")
    public Single<Response> cancelBooking(@PathParam("bookingId")  String bookingId) {
        return bookingService.cancelBooking(bookingId)
                .filter(b -> Boolean.TRUE.equals(b))
                .map(b -> Response.status(Response.Status.NO_CONTENT).build())
                .switchIfEmpty(buildNotFoundResponse("booking not found "));
    }

    private SingleSource<Response> buildNotFoundResponse(String message) {
        return Single.just(Response.status(Response.Status.NOT_FOUND)
                .entity(ErrorsReponse.builder()
                        .error(Error.builder()
                                .code("NOT_FOUND")
                                .message(message)
                                .build()).build())
                .build());
    }

    @PUT
    @Path("/cacheRefresh")
    public Response cacheRefresh() {
        bookingService.refreshCache();
        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
