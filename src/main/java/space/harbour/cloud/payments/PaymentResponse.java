package space.harbour.cloud.payments;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What the client gets back after registering a payment.
 */
public record PaymentResponse(
		String paymentId,
		String storeId,
		CoffeeType coffeeType,
		BigDecimal price,
		String currency,
		String loyaltyCardId,
		Instant registeredAt
) {
	static PaymentResponse from(Payment payment) {
		return new PaymentResponse(
				payment.paymentId(),
				payment.storeId(),
				payment.coffeeType(),
				payment.price(),
				payment.currency(),
				payment.loyaltyCardId(),
				payment.registeredAt()
		);
	}
}
