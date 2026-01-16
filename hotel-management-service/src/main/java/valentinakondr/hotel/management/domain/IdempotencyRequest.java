package valentinakondr.hotel.management.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_request",
        uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_request_request_id", columnNames = "requestId")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID requestId;

    @Column(nullable = false)
    private UUID roomId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}