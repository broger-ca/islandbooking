package org.bin2.island.booking.service;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.pgclient.PgException;
import io.vertx.reactivex.ext.sql.SQLClientHelper;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;
import org.bin2.island.booking.model.Booking;
import org.bin2.island.booking.repository.BookingRepository;

import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class BookingService {
    private static final String PK_VIOLATION = "23505";
    private final PgPool client;
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository,PgPool client) {
        this.bookingRepository = bookingRepository;
        this.client = client;
    }

    public Flowable<LocalDate> getAvailableDates(LocalDate from, LocalDate to) {
        return bookingRepository.bookedDates(client, from, to).toList()
                .map(booked ->from.datesUntil(to).filter(d -> !booked.contains(d)).collect(Collectors.toList()))
                .toFlowable().flatMap(l -> Flowable.fromIterable(l));
    }

    public Single<Boolean> cancelBooking(String bookingId) {
        return client.rxBegin().flatMap(
                tx -> bookingRepository.deleteBookingDates(tx, bookingId)
                        .flatMap(u ->bookingRepository.deleteBooking(tx, bookingId))
                .onErrorResumeNext(e ->
                        tx.rxRollback().andThen(Single.error(e))
                )
                .flatMap(u -> tx.rxCommit().toSingleDefault(u))
        );
    }

    public Maybe<Booking> updateBooking(Booking booking) {
        return client.rxBegin().toMaybe()
                .flatMap(tx ->
                        this.bookingRepository.updateBookingInfo(tx, booking)
                        .onErrorResumeNext((Throwable t)-> tx.rxRollback().andThen(Maybe.error(t)))
                        .flatMap(b ->
                                tx.rxCommit().toSingleDefault(b).toMaybe())
                        );
    }

    /**
     * we are using the db to manage concurrency on the booking entry
     * @param booking
     * @param startDate
     * @param endDate
     * @return
     */
    public Maybe<Booking> tryToBook(Booking booking, LocalDate startDate, LocalDate endDate) {
        final String bookingId = UUID.randomUUID().toString();
        return client.rxBegin().toMaybe()
                .flatMap(tx -> {
                    List<LocalDate> dates = startDate.datesUntil(endDate).collect(Collectors.toList());
                    // we first check if there is any booked date conflicting
                    return this.bookingRepository.bookedDates(tx, startDate, endDate).toList()
                            // if it's not empty it means there is conflict
                            .filter(l ->l.isEmpty())
                            .flatMap(l ->
                                    Flowable.fromIterable(dates)
                                        .flatMapSingle(d -> this.bookingRepository.bookDate(tx, d, bookingId).toSingle())
                                    .lastOrError()
                                            .flatMapMaybe(id ->bookingRepository.createBookingInfo(tx,
                                                    booking.toBuilder().id(id).build()))
                                    .onErrorResumeNext((Throwable t)-> tx.rxRollback().andThen(Maybe.error(t)) )
                                    .flatMap(b -> tx.rxCommit().toSingleDefault(b).toMaybe()));
                    });
    }
}
