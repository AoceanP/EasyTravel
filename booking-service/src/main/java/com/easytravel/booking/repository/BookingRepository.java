package com.easytravel.booking.repository;

import com.easytravel.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * WHAT:   Database access for bookings.
 * WHY:    Extending JpaRepository gives save, findById, findAll and more for
 *         free: Spring writes the implementation at startup, so there is no
 *         SQL and no boilerplate here. Extra queries are derived from method
 *         names: "findByUserIdOrderByCreatedAtDesc" becomes
 *         SELECT * FROM bookings WHERE user_id = ? ORDER BY created_at DESC.
 *
 * @author AoceanP
 */
public interface BookingRepository extends JpaRepository<Booking, UUID>
{
    /**
     * WHAT:   A customer's bookings, newest first.
     * PRE:    userId is not null
     * RETURN: the bookings; empty if there are none
     *
     * @param userId the customer
     * @return the bookings
     */
    List<Booking> findByUserIdOrderByCreatedAtDesc(String userId);
}
