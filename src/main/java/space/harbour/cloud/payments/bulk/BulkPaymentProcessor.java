package space.harbour.cloud.payments.bulk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import space.harbour.cloud.payments.PaymentRequest;
import space.harbour.cloud.sharding.ShardContextHolder;
import space.harbour.cloud.sharding.ShardResolver;

import java.util.List;
import java.util.UUID;

/**
 * Drains a bulk request in the background: for each pending item it creates an
 * entry in the remote payments system (an HTTP POST to {@code app.remote.payments-url},
 * which loops back to this app's synchronous endpoint), then advances the item and
 * the request's progress counter.
 *
 * <p>Runs on a separate thread, so it must establish its own
 * {@link ShardContextHolder} before touching the (sharded) repositories.
 */
@Component
public class BulkPaymentProcessor {

	private static final Logger log = LoggerFactory.getLogger(BulkPaymentProcessor.class);

	private final BulkRequestRepository requestRepository;
	private final BulkPaymentItemRepository itemRepository;
	private final ShardResolver shardResolver;
	private final RestClient restClient;
	private final String remoteUrl;

	public BulkPaymentProcessor(BulkRequestRepository requestRepository,
			BulkPaymentItemRepository itemRepository,
			ShardResolver shardResolver,
			@Value("${app.remote.payments-url}") String remoteUrl) {
		this.requestRepository = requestRepository;
		this.itemRepository = itemRepository;
		this.shardResolver = shardResolver;
		this.restClient = RestClient.create();
		this.remoteUrl = remoteUrl;
	}

	@Async
	public void process(UUID bulkRequestId) {
		ShardContextHolder.set(shardResolver.resolve(bulkRequestId));
		try {
			BulkRequestEntity request = requestRepository.findById(bulkRequestId).orElse(null);
			if (request == null) {
				log.warn("Bulk request {} not found on its shard; skipping", bulkRequestId);
				return;
			}
			request.markProcessing();
			requestRepository.save(request);

			List<BulkPaymentItemEntity> pending =
					itemRepository.findByBulkRequestIdAndStatus(bulkRequestId, BulkItemStatus.PENDING);
			log.info("Processing bulk request {} ({} item(s)) on shard {}",
					bulkRequestId, pending.size(), shardResolver.resolve(bulkRequestId));

			for (BulkPaymentItemEntity item : pending) {
				try {
					sendToRemote(item);
					item.markDone();
				} catch (Exception ex) {
					log.error("Item {} failed: {}", item.getId(), ex.getMessage());
					item.markFailed();
				}
				itemRepository.save(item);
				request.incrementProcessed();
				requestRepository.save(request);
			}

			request.markCompleted();
			requestRepository.save(request);
			log.info("Bulk request {} COMPLETED ({}/{})",
					bulkRequestId, request.getProcessedItems(), request.getTotalItems());
		} finally {
			ShardContextHolder.clear();
		}
	}

	/** Creates the payment entry in the remote system. */
	private void sendToRemote(BulkPaymentItemEntity item) {
		PaymentRequest body = new PaymentRequest(
				item.getCoffeeType(), item.getPrice(), item.getCurrency(), item.getLoyaltyCardId());

		restClient.post()
				.uri(remoteUrl)
				.header("Store-Id", item.getStoreId())
				.header("Idempotency-Key", item.effectiveIdempotencyKey())
				.body(body)
				.retrieve()
				.toBodilessEntity();
	}
}
