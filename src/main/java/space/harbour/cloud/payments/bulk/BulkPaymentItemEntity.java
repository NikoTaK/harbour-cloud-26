package space.harbour.cloud.payments.bulk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import space.harbour.cloud.payments.CoffeeType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One payment line inside a {@link BulkRequestEntity}, stored on the same shard.
 * When processed it is forwarded to the remote payments system.
 */
@Entity
@Table(name = "bulk_payment_items")
public class BulkPaymentItemEntity {

	@Id
	private UUID id;

	@Column(name = "bulk_request_id", nullable = false)
	private UUID bulkRequestId;

	@Column(name = "store_id", nullable = false)
	private String storeId;

	@Column(name = "idempotency_key")
	private String idempotencyKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "coffee_type", nullable = false)
	private CoffeeType coffeeType;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "loyalty_card_id")
	private String loyaltyCardId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BulkItemStatus status;

	protected BulkPaymentItemEntity() {
		// for JPA
	}

	public BulkPaymentItemEntity(UUID id, UUID bulkRequestId, String storeId, String idempotencyKey,
			CoffeeType coffeeType, BigDecimal price, String currency, String loyaltyCardId) {
		this.id = id;
		this.bulkRequestId = bulkRequestId;
		this.storeId = storeId;
		this.idempotencyKey = idempotencyKey;
		this.coffeeType = coffeeType;
		this.price = price;
		this.currency = currency;
		this.loyaltyCardId = loyaltyCardId;
		this.status = BulkItemStatus.PENDING;
	}

	public void markDone() {
		this.status = BulkItemStatus.DONE;
	}

	public void markFailed() {
		this.status = BulkItemStatus.FAILED;
	}

	/**
	 * The idempotency key for the remote call: the caller's transaction id if
	 * present, else this item's own stable id, so retries never duplicate.
	 */
	public String effectiveIdempotencyKey() {
		return (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : id.toString();
	}

	public UUID getId() {
		return id;
	}

	public UUID getBulkRequestId() {
		return bulkRequestId;
	}

	public String getStoreId() {
		return storeId;
	}

	public CoffeeType getCoffeeType() {
		return coffeeType;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public String getCurrency() {
		return currency;
	}

	public String getLoyaltyCardId() {
		return loyaltyCardId;
	}

	public BulkItemStatus getStatus() {
		return status;
	}
}
