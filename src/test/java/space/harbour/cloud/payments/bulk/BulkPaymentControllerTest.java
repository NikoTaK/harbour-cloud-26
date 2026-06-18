package space.harbour.cloud.payments.bulk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the bulk API. The service and async processor are mocked,
 * so no payment processing or remote calls happen; the (sharded) repositories are
 * never touched. The real {@code ShardResolver} runs against the test H2 shards.
 */
@SpringBootTest
class BulkPaymentControllerTest {

	@Autowired
	private WebApplicationContext context;

	@MockitoBean
	private BulkPaymentService service;

	@MockitoBean
	private BulkPaymentProcessor processor;

	private MockMvc mockMvc() {
		return MockMvcBuilders.webAppContextSetup(context).build();
	}

	private static final String VALID_BODY = """
			{
			  "payments": [
			    {"storeId": "store-1", "coffeeType": "LATTE", "price": 3.50, "currency": "EUR", "loyaltyCardId": "card-1"}
			  ]
			}
			""";

	@Test
	void submitReturns202WithTrackingIdAndStartsProcessing() throws Exception {
		mockMvc().perform(post("/api/v1/bulk-payments")
						.contentType(MediaType.APPLICATION_JSON)
						.content(VALID_BODY))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.bulkRequestId", notNullValue()));

		verify(service).createPending(any(UUID.class), any(BulkPaymentRequest.class));
		verify(processor).process(any(UUID.class));
	}

	@Test
	void submitWithNoItemsReturns400() throws Exception {
		mockMvc().perform(post("/api/v1/bulk-payments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"payments\": []}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(processor);
	}

	@Test
	void statusReturnsCurrentProgress() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.getStatus(eq(id)))
				.thenReturn(new BulkStatusResponse(id, BulkRequestStatus.PROCESSING, 10, 4));

		mockMvc().perform(get("/api/v1/bulk-payments/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bulkRequestId").value(id.toString()))
				.andExpect(jsonPath("$.status").value("PROCESSING"))
				.andExpect(jsonPath("$.totalItems").value(10))
				.andExpect(jsonPath("$.processedItems").value(4));
	}

	@Test
	void unknownRequestReturns404() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.getStatus(eq(id)))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No bulk request with id " + id));

		mockMvc().perform(get("/api/v1/bulk-payments/{id}", id))
				.andExpect(status().isNotFound());
	}
}
