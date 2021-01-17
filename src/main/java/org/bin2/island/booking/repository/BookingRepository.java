package org.bin2.island.booking.repository;


import com.google.common.collect.Lists;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.*;
import lombok.extern.slf4j.Slf4j;
import org.bin2.island.booking.model.Booking;

import javax.inject.Singleton;
import java.time.LocalDate;

@Singleton
@Slf4j
public class BookingRepository {


    public Flowable<LocalDate> bookedDates(SqlClient client, LocalDate from, LocalDate to) {
        return client.preparedQuery("select date from \"BOOKING_DATE\" where date BETWEEN $1 and $2")
                .rxExecute(Tuple.of(from, to))
                .toFlowable().flatMap(rows -> Flowable.fromIterable(rows))
                .map(row -> row.getLocalDate("date"));
    }

    public Single<Boolean> deleteBooking(SqlClient client, String bookingId) {
        return client.preparedQuery("Delete from \"BOOKING\" where id = $1")
            .rxExecute(Tuple.of(bookingId))
                .map(r -> r.rowCount()>0);
    }

    public Single<Integer> deleteBookingDates(SqlClient client, String bookingId) {
        return client.preparedQuery("Delete from \"BOOKING_DATE\" where \"bookingId\" = $1")
                .rxExecute(Tuple.of(bookingId))
                .map(RowSet::rowCount);

    }

    public Maybe<String> bookDate(SqlClient client, LocalDate date, String bookingId) {
        return client.preparedQuery("INSERT INTO \"BOOKING_DATE\"( \"date\", \"bookingId\") VALUES ($1, $2)")
                .rxExecute(Tuple.of(date, bookingId))
                .filter(rowSet -> rowSet.rowCount() > 0)
                .map(c -> bookingId);
    }

    public Maybe<Booking> updateBookingInfo(SqlClient client, Booking booking) {
        return client.preparedQuery("UPDATE \"BOOKING\" SET \"email\"=$2, \"firstname\"=$3, \"lastname\"=$4 where id = $1")
                .rxExecute(Tuple.of(booking.getId(), booking.getEmail(), booking.getFirstName(), booking.getLastName()))
                .filter(rowSet -> rowSet.rowCount() > 0)
                .map(c -> booking);
    }

    public Maybe<Booking> createBookingInfo(SqlClient client, Booking booking) {
        return client.preparedQuery("INSERT INTO \"BOOKING\"( \"id\", \"email\", \"firstname\", \"lastname\") VALUES ($1, $2, $3, $4)")
                .rxExecute(Tuple.of(booking.getId(), booking.getEmail(), booking.getFirstName(), booking.getLastName()))
                .filter(rowSet -> rowSet.rowCount() > 0)
                .map(c -> booking);
    }
}
