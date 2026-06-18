package space.harbour.cloud.payments.bulk;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Persists bulk submissions and reports their status. The shard for a request is
 * chosen by the caller (controller / async worker) via {@code ShardContextHolder}
 * before these methods run, so the repositories transparently hit the right shard.
 */
@Service
public class BulkPaymentService {

	private final BulkRequestRepository requestRepository;
	private final BulkPaymentItemRepository itemRepository;
	private final Clock clock;

	public BulkPaymentService(BulkRequestRepository requestRepository,
			BulkPaymentItemRepository itemRepository,
			Clock clock) {
		this.requestRepository = requestRepository;
		this.itemRepository = itemRepository;
		this.clock = clock;
	}

	/** Saves the bulk request and its items as PENDING (atomically). */
	@Transactional
	public void createPending(UUID bulkRequestId, BulkPaymentRequest request) {
		requestRepository.save(
				new BulkRequestEntity(bulkRequestId, request.payments().size(), clock.instant()));

		List<BulkPaymentItemEntity> items = request.payments().stream()
				.map(p -> new BulkPaymentItemEntity(
						UUID.randomUUID(), bulkRequestId, p.storeId(), p.transactionId(),
						p.coffeeType(), p.price(), p.currency(), p.loyaltyCardId()))
				.toList();
		itemRepository.saveAll(items);
	}

	/**
	 * @throws ResponseStatusException 404 if no such bulk request exists on its shard.
	 */
	@Transactional(readOnly = true)
	public BulkStatusResponse getStatus(UUID bulkRequestId) {
		return requestRepository.findById(bulkRequestId)
				.map(BulkStatusResponse::from)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "No bulk request with id " + bulkRequestId));
	}
}
