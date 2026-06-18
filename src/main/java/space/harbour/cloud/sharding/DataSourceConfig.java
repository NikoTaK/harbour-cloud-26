package space.harbour.cloud.sharding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds one physical {@link DataSource} per configured shard and exposes them
 * behind a single {@link ShardingDataSource} (marked {@code @Primary} so JPA uses
 * it). Each shard's schema is created from {@code schema.sql} at startup.
 */
@Configuration
@EnableConfigurationProperties(ShardingProperties.class)
public class DataSourceConfig {

	private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

	@Bean
	@Primary
	public DataSource dataSource(ShardingProperties properties) {
		int count = properties.getCount();
		if (count <= 0 || properties.getDatasource().size() != count) {
			throw new IllegalStateException("app.sharding.count (" + count
					+ ") must match the number of app.sharding.datasource entries ("
					+ properties.getDatasource().size() + ")");
		}

		Map<Object, Object> shards = new HashMap<>();
		for (int i = 0; i < count; i++) {
			ShardingProperties.DataSource cfg = properties.getDatasource().get(i);
			if (cfg == null) {
				throw new IllegalStateException("Missing app.sharding.datasource." + i);
			}
			DataSource shard = DataSourceBuilder.create()
					.url(cfg.getUrl())
					.username(cfg.getUsername())
					.password(cfg.getPassword())
					.build();
			initSchema(shard, i);
			shards.put(i, shard);
		}

		ShardingDataSource routing = new ShardingDataSource();
		routing.setTargetDataSources(shards);
		routing.setDefaultTargetDataSource(shards.get(0));
		routing.afterPropertiesSet();
		return routing;
	}

	private void initSchema(DataSource shard, int index) {
		ResourceDatabasePopulator populator =
				new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
		populator.execute(shard);
		log.info("Initialised schema on shard {}", index);
	}
}
