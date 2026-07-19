package com.example.webhook;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebhookController.class)
class WebhookControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void receivesPostAndConfirms() throws Exception {
		mockMvc.perform(post("/webhook/test").content("hello"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("received"))
				.andExpect(jsonPath("$.method").value("POST"))
				.andExpect(jsonPath("$.path").value("/webhook/test"))
				.andExpect(jsonPath("$.bytesReceived").value(5));
	}

	@Test
	void acceptsAnyPathAndMethod() throws Exception {
		mockMvc.perform(get("/webhook/a/b/c"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.method").value("GET"))
				.andExpect(jsonPath("$.path").value("/webhook/a/b/c"));
	}

	@Test
	void largeBodyIsDrainedAndCounted() throws Exception {
		String big = "A".repeat(1_048_576);
		mockMvc.perform(post("/webhook/big").content(big))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bytesReceived").value(1_048_576));
	}
}
