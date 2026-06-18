package space.harbour.cloud.sharding;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Decides which shard owns a given bulk request.
 *
 * <p>{@code Math.floorMod(id.hashCode(), shardCount)} - a pure function of the
 * id, so every read and write for a request routes to the same shard.
 * {@code floorMod} (rather than {@code abs(..) % n}) keeps the index non-negative
 * even for the edge-case hash {@code Integer.MIN_VALUE}.
 */
@Component
public class ShardResolver {

	private final int shardCount;

	public ShardResolver(ShardingProperties properties) {
		this.shardCount = properties.getCount();
	}

	public int resolve(UUID bulkRequestId) {
		return Math.floorMod(bulkRequestId.hashCode(), shardCount);
	}

	public int shardCount() {
		return shardCount;
	}
}
