package space.harbour.cloud.payments.bulk;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.harbour.cloud.sharding.ShardContextHolder;
import space.harbour.cloud.sharding.ShardResolver;

import java.util.UUID;

/**
 * Asynchronous bulk payment API.
 *
 * <p>{@code POST} stores the batch and returns a tracking id immediately
 * ({@code 202 Accepted}); the work happens in the background. {@code GET} reports
 * progress. Both pick the shard from the bulk request id before touching the
 * database (see {@link ShardResolver} / {@link ShardContextHolder}).
 */
@RestController
@RequestMapping("/api/v1/bulk-payments")
public class BulkPaymentController {

	private final BulkPaymentService service;
	private final BulkPaymentProcessor processor;
	private final ShardResolver shardResolver;

	public BulkPaymentController(BulkPaymentService service,
			BulkPaymentProcessor processor,
			ShardResolver shardResolver) {
		this.service = service;
		this.processor = processor;
		this.shardResolver = shardResolver;
	}

	@PostMapping
	public ResponseEntity<SubmitBulkResponse> submit(@Valid @RequestBody BulkPaymentRequest request) {
		UUID bulkRequestId = UUID.randomUUID();

		ShardContextHolder.set(shardResolver.resolve(bulkRequestId));
		try {
			service.createPending(bulkRequestId, request);
		} finally {
			ShardContextHolder.clear();
		}

		// Hand off to the background worker (it sets its own shard context).
		processor.process(bulkRequestId);

		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(new SubmitBulkResponse(bulkRequestId));
	}

	@GetMapping("/{bulkRequestId}")
	public BulkStatusResponse status(@PathVariable UUID bulkRequestId) {
		ShardContextHolder.set(shardResolver.resolve(bulkRequestId));
		try {
			return service.getStatus(bulkRequestId);
		} finally {
			ShardContextHolder.clear();
		}
	}
}
