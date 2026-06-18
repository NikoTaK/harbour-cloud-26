package space.harbour.cloud.sharding;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * A {@link javax.sql.DataSource} that hands out a connection from the shard named
 * by {@link ShardContextHolder}. JPA/Spring Data use it like any other datasource;
 * the shard switch happens transparently when a connection is requested.
 */
public class ShardingDataSource extends AbstractRoutingDataSource {

	@Override
	protected Object determineCurrentLookupKey() {
		// null falls back to the configured default target (shard 0) - used during
		// startup (e.g. Hibernate dialect detection) before any shard is set.
		return ShardContextHolder.get();
	}
}
