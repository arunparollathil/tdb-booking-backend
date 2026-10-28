package com.tdb.booking.repository;

import com.tdb.booking.entity.DarshanBooking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DarshanBookingRepository extends JpaRepository<DarshanBooking, UUID> {

    Optional<DarshanBooking> findByIdempotencyKey(UUID idempotencyKey);

    boolean existsByIdempotencyKey(UUID idempotencyKey);

    Optional<DarshanBooking> findByReceiptNumber(String receiptNumber);

    boolean existsByReceiptNumber(String receiptNumber);

    List<DarshanBooking> findByDarshanSlotId(UUID darshanSlotId);

    List<DarshanBooking> findByPhoneNumber(String phoneNumber);
}
