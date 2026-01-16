package valentinakondr.booking.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import valentinakondr.booking.domain.User;
import valentinakondr.booking.domain.booking.Booking;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUserOrderByCreatedAtDesc(User user);
}

