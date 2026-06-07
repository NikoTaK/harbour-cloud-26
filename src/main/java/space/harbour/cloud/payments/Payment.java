package space.harbour.cloud.payments;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A coffee payment that has been accepted and recorded by the system.
 */
public record Payment(
		String paymentId,
		String storeId,
		CoffeeType coffeeType,
		BigDecimal price,
		String currency,
		String loyaltyCardId,
		String idempotencyKey,
		Instant registeredAt
) {
}
