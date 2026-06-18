package space.harbour.cloud.payments.bulk;

import java.util.UUID;

/**
 * Returned immediately from a bulk submission - the tracking id the client polls.
 */
public record SubmitBulkResponse(UUID bulkRequestId) {
}
