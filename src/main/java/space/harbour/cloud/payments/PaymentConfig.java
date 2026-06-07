package space.harbour.cloud.payments;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PaymentConfig {

	/**
	 * A system-UTC clock. Injected into {@link PaymentService} so that tests can
	 * supply a fixed clock and assert on timestamps deterministically.
	 */
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
