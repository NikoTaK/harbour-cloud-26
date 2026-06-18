package space.harbour.cloud.sharding;

/**
 * Holds the target shard index for the current thread.
 *
 * <p>{@link ShardingDataSource} reads this when it needs to pick a physical
 * database connection. Callers must {@link #set(int)} the shard before touching a
 * sharded repository and {@link #clear()} it afterwards (the {@code @Async}
 * worker runs on its own thread, so it sets its own value).
 */
public final class ShardContextHolder {

	private static final ThreadLocal<Integer> CONTEXT = new ThreadLocal<>();

	private ShardContextHolder() {
	}

	public static void set(int shardIndex) {
		CONTEXT.set(shardIndex);
	}

	/** The current shard index, or {@code null} if none is set. */
	public static Integer get() {
		return CONTEXT.get();
	}

	public static void clear() {
		CONTEXT.remove();
	}
}
