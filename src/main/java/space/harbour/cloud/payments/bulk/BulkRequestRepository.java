package space.harbour.cloud.payments.bulk;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface BulkRequestRepository extends CrudRepository<BulkRequestEntity, UUID> {
}
