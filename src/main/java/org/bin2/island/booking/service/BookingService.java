package org.bin2.island.booking.service;

import com.google.common.collect.Sets;
import io.micronaut.caffeine.cache.AsyncCache;
import io.micronaut.caffeine.cache.Cache;
import io.micronaut.caffeine.cache.Caffeine;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.pgclient.PgException;
import io.vertx.reactivex.pgclient.PgPool;
import lombok.extern.slf4j.Slf4j;
import org.bin2.island.booking.model.Booking;
import org.bin2.island.booking.repository.BookingRepository;

import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class BookingService {
    private static final String PK_VIOLATION = "23505";
    private final PgPool client;
    private final BookingRepository bookingRepository;
    private final BookingClient bookingClient;

    private volatile Single<Set<LocalDate>> cacheBookedDates;

    public BookingService(BookingRepository bookingRepository,PgPool client, BookingClient bookingClient) {
        this.bookingRepository = bookingRepository;
        this.client = client;
        this.cacheBookedDates = bookedDates();
        this.bookingClient = bookingClient;
    }

    public Flowable<LocalDate> getAvailableDates(LocalDate from, LocalDate to) {
        return cacheBookedDates
                .map(booked ->from.datesUntil(to).filter(d -> !booked.contains(d)).collect(Collectors.toList()))
                .toFlowable().flatMap(l -> Flowable.fromIterable(l));
    }


    /**
     * invalidate on update event
     */
    public void refreshCache() {
        this.cacheBookedDates = bookedDates();
    }

    private Single<Set<LocalDate>> bookedDates() {
        LocalDate minDate = LocalDate.now().plus(1, ChronoUnit.DAYS);
        LocalDate maxDate = minDate.plus(1, ChronoUnit.MONTHS);
        return bookingRepository.bookedDates(client, minDate, maxDate).toList()
                .map(l -> (Set<LocalDate>)Sets.newTreeSet(l)).cache();
    }

    public Single<Boolean> cancelBooking(String bookingId) {
        return client.rxBegin().flatMap(
                tx -> bookingRepository.deleteBookingDates(tx, bookingId)
                        .flatMap(u ->bookingRepository.deleteBooking(tx, bookingId))
                .flatMap(u -> tx.rxCommit().doOnComplete(()-> triggerBookingEvent(bookingId)).toSingleDefault(u)
                .onErrorResumeNext(e ->
                        tx.rxRollback().andThen(Single.error(e))
                ))
        );
    }

    public Maybe<Booking> updateBooking(Booking booking) {
        return client.rxBegin().toMaybe()
                .flatMap(tx ->
                        this.bookingRepository.updateBookingInfo(tx, booking)
                        .flatMap(b ->
                                tx.rxCommit().toSingleDefault(b).toMaybe())
                                .onErrorResumeNext((Throwable t)-> tx.rxRollback().andThen(Maybe.error(t)))
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
                    //if there is a concurrent transaction inserting at the same time it will faire with a  PK_VIOLATION violation error
                    return this.bookingRepository.bookedDates(tx, startDate, endDate).toList()
                            // if it's not empty it means there is conflict
                            .filter(l -> l.isEmpty())
                            .flatMap(l ->
                                    Flowable.fromIterable(dates)
                                            .flatMapSingle(d -> this.bookingRepository.bookDate(tx, d, bookingId).toSingle())
                                            .lastOrError()
                                            .flatMap(id -> bookingRepository.createBookingInfo(tx,
                                                    booking.toBuilder().id(id).build()))
                                            .flatMapMaybe(b -> tx.rxCommit()
                                                    .doOnComplete(()-> triggerBookingEvent(bookingId))
                                                    .toSingleDefault(b).toMaybe()))
                                            .onErrorResumeNext((Throwable t) -> {
                                                if (isConstraintError(t)) {
                                                    // in this case the transaction has allready been rollback
                                                    return Maybe.empty();
                                                } else {
                                                    return tx.rxRollback().andThen(Maybe.error(t));
                                                }
                                            });
                });
    }

    private void triggerBookingEvent(String bookingId) {
        //bookingClient.sendEvent(bookingId);
        // not needed if kafka properly setup
        refreshCache();
    }

    private boolean isConstraintError(Throwable t) {
        return t instanceof PgException && PK_VIOLATION.equals(((PgException)t).getCode());
    }
}
