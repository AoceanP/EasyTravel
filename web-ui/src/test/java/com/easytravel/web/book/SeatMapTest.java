package com.easytravel.web.book;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WHAT:   Tests for the seat grid.
 *
 * @author AoceanP
 */
class SeatMapTest
{
    /**
     * WHAT:   Grid shape and row numbers per cabin.
     * WHY:    5 rows of A-C and D-F, starting where each cabin starts.
     * RETURN: nothing; fails if the shape is wrong
     */
    @Test
    void shapeAndRowNumbers()
    {
        List<SeatMap.Row> rows = SeatMap.rows("BA249", Cabin.BUSINESS, null, SeatMap.State.SELECTED);
        assertThat(rows).hasSize(SeatMap.ROWS);
        assertThat(rows.getFirst().number()).isEqualTo(6);
        assertThat(rows.getFirst().left()).extracting(SeatMap.Seat::id).containsExactly("6A", "6B", "6C");
        assertThat(rows.getFirst().right()).extracting(SeatMap.Seat::id).containsExactly("6D", "6E", "6F");
    }

    /**
     * WHAT:   The same flight twice.
     * WHY:    Taken seats must not change between requests.
     * RETURN: nothing; fails if the maps differ
     */
    @Test
    void stableForAFlight()
    {
        assertThat(SeatMap.rows("DL215", Cabin.ECONOMY, null, SeatMap.State.SELECTED))
                .isEqualTo(SeatMap.rows("DL215", Cabin.ECONOMY, null, SeatMap.State.SELECTED));
    }

    /**
     * WHAT:   A chosen free seat, then the same seat once booked.
     * WHY:    It shows its letter, and its state follows the booking.
     * RETURN: nothing; fails if the chosen seat looks wrong
     */
    @Test
    void chosenSeatIsMarked()
    {
        String free = firstFree("UA930");
        SeatMap.Seat picked = SeatMap.find(SeatMap.rows("UA930", Cabin.FIRST, free, SeatMap.State.SELECTED), free);
        assertThat(picked.state()).isEqualTo(SeatMap.State.SELECTED);
        assertThat(picked.mark()).isEqualTo(free.substring(free.length() - 1));
        assertThat(SeatMap.find(SeatMap.rows("UA930", Cabin.FIRST, free, SeatMap.State.YOURS), free).css())
                .contains("seat-yours");
    }

    /**
     * WHAT:   A taken seat passed as "chosen".
     * WHY:    Taken wins: someone else has it.
     * RETURN: nothing; fails if the taken seat looks chosen
     */
    @Test
    void takenSeatStaysTaken()
    {
        String taken = firstTaken("VS045");
        SeatMap.Seat s = SeatMap.find(SeatMap.rows("VS045", Cabin.FIRST, taken, SeatMap.State.SELECTED), taken);
        assertThat(s.isTaken()).isTrue();
        assertThat(s.aria()).endsWith("taken");
    }

    /**
     * WHAT:   An id that is not on the map.
     * RETURN: nothing; fails unless find returns null
     */
    @Test
    void unknownSeatIsNull()
    {
        assertThat(SeatMap.find(SeatMap.rows("BA249", Cabin.FIRST, null, SeatMap.State.SELECTED), "99Z")).isNull();
    }

    /**
     * WHAT:   The first free seat in first class on a flight.
     * PRE:    the flight has one
     * RETURN: its id
     *
     * @param flight the flight
     * @return the seat id
     */
    private static String firstFree(String flight)
    {
        return SeatMap.rows(flight, Cabin.FIRST, null, SeatMap.State.SELECTED).stream()
                .flatMap(r -> java.util.stream.Stream.concat(r.left().stream(), r.right().stream()))
                .filter(s -> !s.isTaken()).findFirst().orElseThrow().id();
    }

    /**
     * WHAT:   The first taken seat in first class on a flight.
     * PRE:    the flight has one
     * RETURN: its id
     *
     * @param flight the flight
     * @return the seat id
     */
    private static String firstTaken(String flight)
    {
        return SeatMap.rows(flight, Cabin.FIRST, null, SeatMap.State.SELECTED).stream()
                .flatMap(r -> java.util.stream.Stream.concat(r.left().stream(), r.right().stream()))
                .filter(SeatMap.Seat::isTaken).findFirst().orElseThrow().id();
    }
}
