package space.harbour.cloud.sharding;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for shard routing - no Spring context, no database.
 */
class ShardResolverTest {

	private static ShardResolver resolverWithCount(int count) {
		ShardingProperties props = new ShardingProperties();
		props.setCount(count);
		return new ShardResolver(props);
	}

	@Test
	void routingIsDeterministicForTheSameId() {
		ShardResolver resolver = resolverWithCount(4);
		UUID id = UUID.randomUUID();
		assertThat(resolver.resolve(id)).isEqualTo(resolver.resolve(id));
	}

	@Test
	void indexAlwaysWithinShardRange() {
		ShardResolver resolver = resolverWithCount(3);
		for (int i = 0; i < 1000; i++) {
			int shard = resolver.resolve(UUID.randomUUID());
			assertThat(shard).isBetween(0, 2);
		}
	}

	@Test
	void spreadsAcrossAllConfiguredShards() {
		ShardResolver resolver = resolverWithCount(2);
		Set<Integer> seen = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			seen.add(resolver.resolve(UUID.randomUUID()));
		}
		assertThat(seen).containsExactlyInAnyOrder(0, 1);
	}

	@Test
	void changingShardCountChangesAssignments() {
		UUID id = UUID.randomUUID();
		int twoShards = resolverWithCount(2).resolve(id);
		int sevenShards = resolverWithCount(7).resolve(id);
		assertThat(twoShards).isEqualTo(Math.floorMod(id.hashCode(), 2));
		assertThat(sevenShards).isEqualTo(Math.floorMod(id.hashCode(), 7));
	}
}
