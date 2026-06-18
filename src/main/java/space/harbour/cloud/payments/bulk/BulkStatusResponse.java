package space.harbour.cloud.payments.bulk;

import java.util.UUID;

/**
 * The status of a bulk request, returned from the polling endpoint.
 */
public record BulkStatusResponse(
		UUID bulkRequestId,
		BulkRequestStatus status,
		int totalItems,
		int processedItems
) {
	static BulkStatusResponse from(BulkRequestEntity entity) {
		return new BulkStatusResponse(
				entity.getId(),
				entity.getStatus(),
				entity.getTotalItems(),
				entity.getProcessedItems());
	}
}
