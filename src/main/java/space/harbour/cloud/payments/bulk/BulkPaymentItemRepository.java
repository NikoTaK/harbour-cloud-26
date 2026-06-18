package space.harbour.cloud.payments.bulk;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface BulkPaymentItemRepository extends CrudRepository<BulkPaymentItemEntity, UUID> {

	/** Items of a bulk request that still need processing. */
	List<BulkPaymentItemEntity> findByBulkRequestIdAndStatus(UUID bulkRequestId, BulkItemStatus status);
}
