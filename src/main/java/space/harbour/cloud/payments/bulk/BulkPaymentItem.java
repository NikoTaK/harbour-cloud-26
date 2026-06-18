package space.harbour.cloud.payments.bulk;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import space.harbour.cloud.payments.CoffeeType;

import java.math.BigDecimal;

/**
 * A single payment line in a bulk submission. Validation mirrors the synchronous
 * {@code PaymentRequest}, plus {@code storeId} and an optional {@code transactionId}
 * that travelled as HTTP headers in the single-payment API.
 */
public record BulkPaymentItem(

		@NotNull(message = "storeId is required")
		String storeId,

		String transactionId,

		@NotNull(message = "coffeeType is required")
		CoffeeType coffeeType,

		@NotNull(message = "price is required")
		@DecimalMin(value = "0.0", inclusive = false, message = "price must be greater than zero")
		@Digits(integer = 10, fraction = 2, message = "price may have at most 2 decimal places")
		BigDecimal price,

		@NotNull(message = "currency is required")
		@Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO-4217 code, e.g. EUR")
		String currency,

		@NotNull(message = "loyaltyCardId is required")
		String loyaltyCardId
) {
}
