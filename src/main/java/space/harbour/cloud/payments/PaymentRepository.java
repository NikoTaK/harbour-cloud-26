package space.harbour.cloud.payments;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for payments.
 *
 * <p>This is intentionally simple - the course swaps this out for a real
 * database later. The interesting part is the idempotency index: a payment
 * is looked up by the (storeId, idempotencyKey) pair so that a retried
 * request returns the original payment instead of creating a duplicate.
 */
@Repository
public class PaymentRepository {

	private final Map<String, Payment> paymentsById = new ConcurrentHashMap<>();
	private final Map<IdempotencyKey, Payment> paymentsByIdempotencyKey = new ConcurrentHashMap<>();

	/**
	 * Stores the payment only if no payment exists yet for this
	 * (storeId, idempotencyKey) pair, atomically.
	 *
	 * @return the stored payment, or the pre-existing one if this was a retry.
	 */
	public Payment saveIfAbsent(Payment payment) {
		IdempotencyKey key = new IdempotencyKey(payment.storeId(), payment.idempotencyKey());
		Payment existing = paymentsByIdempotencyKey.putIfAbsent(key, payment);
		if (existing != null) {
			return existing;
		}
		paymentsById.put(payment.paymentId(), payment);
		return payment;
	}

	public Optional<Payment> findById(String paymentId) {
		return Optional.ofNullable(paymentsById.get(paymentId));
	}

	public List<Payment> findAll() {
		return List.copyOf(paymentsById.values());
	}

	public List<Payment> findByStoreId(String storeId) {
		return paymentsById.values().stream()
				.filter(p -> p.storeId().equals(storeId))
				.toList();
	}

	private record IdempotencyKey(String storeId, String idempotencyKey) {
	}
}
