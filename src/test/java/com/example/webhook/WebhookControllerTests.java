package com.example.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebhookController.class)
@Import(RequestStore.class)
class WebhookControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RequestStore store;

	@BeforeEach
	void reset() {
		store.clear();
	}

	@Test
	void receivesPostConfirmsAndCaptures() throws Exception {
		mockMvc.perform(post("/webhook/test").content("hello"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("received"))
				.andExpect(jsonPath("$.method").value("POST"))
				.andExpect(jsonPath("$.path").value("/webhook/test"))
				.andExpect(jsonPath("$.bytesReceived").value(5));

		List<CapturedRequest> captured = store.list();
		assertThat(captured).hasSize(1);
		assertThat(captured.get(0).method()).isEqualTo("POST");
		assertThat(captured.get(0).path()).isEqualTo("/webhook/test");
		assertThat(captured.get(0).body()).isEqualTo("hello");
		assertThat(captured.get(0).bytesReceived()).isEqualTo(5);
		assertThat(captured.get(0).bodyTruncated()).isFalse();
	}

	@Test
	void acceptsAnyPathAndMethod() throws Exception {
		mockMvc.perform(get("/webhook/a/b/c"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.method").value("GET"))
				.andExpect(jsonPath("$.path").value("/webhook/a/b/c"));
	}

	@Test
	void largeBodyIsDrainedCountedAndCaptureBounded() throws Exception {
		String big = "A".repeat(1_048_576);
		mockMvc.perform(post("/webhook/big").content(big))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bytesReceived").value(1_048_576));

		CapturedRequest captured = store.list().get(0);
		assertThat(captured.bytesReceived()).isEqualTo(1_048_576);
		// Retained body is capped at 64 KB even though 1 MB was received.
		assertThat(captured.body().length()).isEqualTo(64 * 1024);
		assertThat(captured.bodyTruncated()).isTrue();
	}

	@Test
	void keepsNewestFirst() throws Exception {
		mockMvc.perform(post("/webhook/first").content("1"));
		mockMvc.perform(post("/webhook/second").content("2"));

		List<CapturedRequest> captured = store.list();
		assertThat(captured.get(0).path()).isEqualTo("/webhook/second");
		assertThat(captured.get(1).path()).isEqualTo("/webhook/first");
	}
}
