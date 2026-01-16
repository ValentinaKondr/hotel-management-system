package valentinakondr.hotel.management.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import valentinakondr.hotel.management.domain.IdempotencyRequest;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRequestRepository extends JpaRepository<IdempotencyRequest, UUID> {
    boolean existsByRequestId(UUID requestId);
    Optional<IdempotencyRequest> findByRequestId(UUID requestId);
    void deleteByRequestId(UUID requestId);
}
