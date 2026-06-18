package space.harbour.cloud.payments.bulk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks one bulk payment request and its progress. Lives on the shard chosen by
 * {@code hash(id) % shardCount}, alongside its {@link BulkPaymentItemEntity}s.
 */
@Entity
@Table(name = "bulk_requests")
public class BulkRequestEntity {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BulkRequestStatus status;

	@Column(name = "total_items", nullable = false)
	private int totalItems;

	@Column(name = "processed_items", nullable = false)
	private int processedItems;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected BulkRequestEntity() {
		// for JPA
	}

	public BulkRequestEntity(UUID id, int totalItems, Instant createdAt) {
		this.id = id;
		this.status = BulkRequestStatus.PENDING;
		this.totalItems = totalItems;
		this.processedItems = 0;
		this.createdAt = createdAt;
	}

	public void markProcessing() {
		this.status = BulkRequestStatus.PROCESSING;
	}

	public void markCompleted() {
		this.status = BulkRequestStatus.COMPLETED;
	}

	public void incrementProcessed() {
		this.processedItems++;
	}

	public UUID getId() {
		return id;
	}

	public BulkRequestStatus getStatus() {
		return status;
	}

	public int getTotalItems() {
		return totalItems;
	}

	public int getProcessedItems() {
		return processedItems;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
