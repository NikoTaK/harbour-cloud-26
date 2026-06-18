package space.harbour.cloud.payments.bulk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * The body of a bulk submission: a non-empty list of payment lines.
 */
public record BulkPaymentRequest(

		@NotEmpty(message = "payments must contain at least one item")
		@Valid
		List<BulkPaymentItem> payments
) {
}
