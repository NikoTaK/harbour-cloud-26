package space.harbour.cloud.payments.bulk;

/**
 * Lifecycle of a bulk payment request: {@code PENDING} on submit,
 * {@code PROCESSING} while the background worker drains its items,
 * {@code COMPLETED} when all are handled, {@code FAILED} if it could not run.
 */
public enum BulkRequestStatus {
	PENDING,
	PROCESSING,
	COMPLETED,
	FAILED
}
