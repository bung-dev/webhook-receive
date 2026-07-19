package com.example.webhook;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON API backing the inspector UI ({@code static/index.html}).
 */
@RestController
@RequestMapping("/api")
public class InspectorController {

	private final RequestStore store;

	public InspectorController(RequestStore store) {
		this.store = store;
	}

	/** Captured requests, newest first. */
	@GetMapping("/requests")
	public List<CapturedRequest> requests() {
		return store.list();
	}

	/** Clears all captured requests. */
	@DeleteMapping("/requests")
	public Map<String, Object> clear() {
		store.clear();
		return Map.of("cleared", true);
	}
}
