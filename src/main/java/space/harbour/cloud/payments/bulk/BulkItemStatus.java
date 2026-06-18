package space.harbour.cloud.payments.bulk;

/**
 * Lifecycle of a single payment inside a bulk request.
 */
public enum BulkItemStatus {
	PENDING,
	DONE,
	FAILED
}
