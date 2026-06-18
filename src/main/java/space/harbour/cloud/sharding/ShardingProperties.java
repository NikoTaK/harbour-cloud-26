package space.harbour.cloud.sharding;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Static shard configuration, bound from {@code app.sharding.*}.
 *
 * <pre>
 * app.sharding.count=2
 * app.sharding.datasource.0.url=...
 * app.sharding.datasource.1.url=...
 * </pre>
 */
@ConfigurationProperties(prefix = "app.sharding")
public class ShardingProperties {

	private int count;

	/** Per-shard connection details, keyed by shard index (0..count-1). */
	private Map<Integer, DataSource> datasource = new HashMap<>();

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Map<Integer, DataSource> getDatasource() {
		return datasource;
	}

	public void setDatasource(Map<Integer, DataSource> datasource) {
		this.datasource = datasource;
	}

	public static class DataSource {
		private String url;
		private String username;
		private String password;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
}
