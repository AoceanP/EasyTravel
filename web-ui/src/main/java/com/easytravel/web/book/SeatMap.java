package com.easytravel.web.book;

import java.util.ArrayList;
import java.util.List;

/**
 * WHAT:   The seat grid for one flight and cabin: 5 rows of A-C, aisle, D-F.
 * WHY:    Inventory has no seat data yet, so which seats are taken is a fixed
 *         pattern derived from the flight number: the same flight always shows
 *         the same map. The chosen seat is a preference on the ticket until seat
 *         holds exist in inventory-service (then this class reads real seats).
 *
 * @author AoceanP
 */
public final class SeatMap
{
    /** WHAT: rows shown per cabin. */
    static final int ROWS = 5;

    /** WHAT: the seat letters; an aisle sits between C and D. */
    private static final String[] LETTERS = { "A", "B", "C", "D", "E", "F" };

    /**
     * WHAT:   How one seat looks.
     */
    public enum State
    {
        FREE("seat"), TAKEN("seat seat-taken"), SELECTED("seat seat-selected"), YOURS("seat seat-yours");

        /** WHAT: the CSS classes for this state. */
        private final String css;

        /**
         * WHAT:   Creates a state.
         * PRE:    none
         *
         * @param css CSS classes
         */
        State(String css)
        {
            this.css = css;
        }
    }

    /**
     * WHAT:   One seat.
     *
     * @param id     e.g. "21C"
     * @param letter e.g. "C"
     * @param state  how it looks
     * @param css    its CSS classes
     * @param aria   what a screen reader says, e.g. "Seat 21C, free"
     * @param mark   the text on the seat (its letter once chosen, else empty)
     */
    public record Seat(String id, String letter, State state, String css, String aria, String mark)
    {
        /**
         * WHAT:   Whether someone else already has this seat.
         * PRE:    none
         * RETURN: true if taken
         *
         * @return whether taken
         */
        public boolean isTaken()
        {
            return state == State.TAKEN;
        }
    }

    /**
     * WHAT:   One row: three seats left of the aisle, three right.
     *
     * @param number the row number
     * @param left   seats A-C
     * @param right  seats D-F
     */
    public record Row(int number, List<Seat> left, List<Seat> right)
    {
    }

    /**
     * WHAT:   Not instantiable.
     * WHY:    Static builder only.
     */
    private SeatMap()
    {
    }

    /**
     * WHAT:   Builds the grid.
     * PRE:    flight and cabin are not null; chosenState is SELECTED or YOURS
     * RETURN: ROWS rows of 6 seats
     *
     * @param flight      the flight number
     * @param cabin       the cabin
     * @param chosen      the chosen seat id, or null
     * @param chosenState how to show the chosen seat
     * @return the rows
     */
    public static List<Row> rows(String flight, Cabin cabin, String chosen, State chosenState)
    {
        List<Row> rows = new ArrayList<>(ROWS);
        for (int r = 0; r < ROWS; r++)
        {
            int number = cabin.getFirstRow() + r;
            List<Seat> seats = new ArrayList<>(LETTERS.length);
            for (int c = 0; c < LETTERS.length; c++)
            {
                String id = number + LETTERS[c];
                State state = isTaken(flight, r, c) ? State.TAKEN
                        : id.equals(chosen) ? chosenState
                        : State.FREE;
                seats.add(seat(id, LETTERS[c], state));
            }
            rows.add(new Row(number, List.copyOf(seats.subList(0, 3)), List.copyOf(seats.subList(3, 6))));
        }
        return rows;
    }

    /**
     * WHAT:   Whether a seat is taken on a flight.
     * WHY:    Deterministic, so the map is stable between requests and in tests.
     * PRE:    flight is not null
     * RETURN: true for about one seat in five
     *
     * @param flight the flight number
     * @param row    the row index in the cabin (0-based)
     * @param col    the seat index (0 = A)
     * @return whether taken
     */
    static boolean isTaken(String flight, int row, int col)
    {
        int sum = 0;
        for (char ch : flight.toCharArray())
        {
            sum += ch;
        }
        return (row * 7 + col * 3 + sum) % 5 == 0;
    }

    /**
     * WHAT:   Finds a seat by id in a grid.
     * PRE:    rows is not null
     * RETURN: the seat, or null if it is not in the grid
     *
     * @param rows the grid
     * @param id   the seat id
     * @return the seat
     */
    public static Seat find(List<Row> rows, String id)
    {
        for (Row row : rows)
        {
            for (Seat s : row.left())
            {
                if (s.id().equals(id)) return s;
            }
            for (Seat s : row.right())
            {
                if (s.id().equals(id)) return s;
            }
        }
        return null;
    }

    /**
     * WHAT:   Builds one seat with its look and label.
     * PRE:    none
     * RETURN: the seat
     *
     * @param id     seat id
     * @param letter seat letter
     * @param state  how it looks
     * @return the seat
     */
    private static Seat seat(String id, String letter, State state)
    {
        String words = switch (state)
        {
            case FREE -> "free";
            case TAKEN -> "taken";
            case SELECTED -> "selected";
            case YOURS -> "yours";
        };
        boolean marked = state == State.SELECTED || state == State.YOURS;
        return new Seat(id, letter, state, state.css, "Seat " + id + ", " + words, marked ? letter : "");
    }
}
