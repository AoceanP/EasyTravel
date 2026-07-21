package com.easytravel.booking.repository;

import com.easytravel.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository. By simply EXTENDING JpaRepository<Booking, Long>, we get
 * save/findById/findAll/delete/etc. for free — Spring generates the implementation at
 * runtime. We write ZERO SQL and ZERO boilerplate.
 *
 * <p>The magic extra: Spring Data derives queries from METHOD NAMES. "findByUserId"
 * is parsed into "SELECT * FROM bookings WHERE user_id = ?". No implementation needed —
 * the name IS the query. This is the "derived query" feature and it's worth demoing.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(String userId);
}