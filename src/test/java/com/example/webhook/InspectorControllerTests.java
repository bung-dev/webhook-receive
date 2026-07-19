package com.example.webhook;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InspectorController.class)
@Import(RequestStore.class)
class InspectorControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RequestStore store;

	@BeforeEach
	void reset() {
		store.clear();
	}

	@Test
	void listsCapturedRequestsNewestFirst() throws Exception {
		store.record("POST", "/webhook/a", null, "127.0.0.1", "text/plain",
				Map.of("Host", "localhost"), 3, "abc", false, Instant.now());

		mockMvc.perform(get("/api/requests"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].method").value("POST"))
				.andExpect(jsonPath("$[0].path").value("/webhook/a"))
				.andExpect(jsonPath("$[0].body").value("abc"));
	}

	@Test
	void clearEmptiesTheStore() throws Exception {
		store.record("GET", "/webhook/x", null, "127.0.0.1", null,
				Map.of(), 0, "", false, Instant.now());

		mockMvc.perform(delete("/api/requests"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cleared").value(true));

		mockMvc.perform(get("/api/requests"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}
}
