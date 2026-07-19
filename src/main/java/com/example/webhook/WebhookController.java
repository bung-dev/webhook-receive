package com.example.webhook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Receiver endpoint. Accepts any HTTP method under {@code /webhook/**} - the
 * role SAP BTP Integration Suite calls - captures the request for the inspector
 * UI, and returns 200 with a small receipt.
 *
 * <p><b>Memory note:</b> the request body is drained straight off the servlet
 * {@link InputStream} into a per-thread reusable buffer. Only the first
 * {@value #BODY_CAPTURE_LIMIT} bytes are kept for display; anything beyond that
 * is counted and discarded, so a large payload never sits whole on the heap.
 */
@RestController
public class WebhookController {

	/** Size of the reusable drain buffer, per request thread. */
	private static final int BUFFER_SIZE = 8 * 1024;
	/** Upper bound on the body bytes retained for the inspector UI. */
	private static final int BODY_CAPTURE_LIMIT = 64 * 1024;

	/**
	 * One 8 KB buffer per request thread, reused across requests. Tomcat's thread
	 * pool is bounded, so drain-buffer allocation is capped at (poolSize * 8 KB).
	 */
	private static final ThreadLocal<byte[]> DRAIN_BUFFER =
			ThreadLocal.withInitial(() -> new byte[BUFFER_SIZE]);

	private final RequestStore store;

	public WebhookController(RequestStore store) {
		this.store = store;
	}

	@RequestMapping("/webhook/**")
	public ResponseEntity<ReceiptResponse> receive(HttpServletRequest request) throws IOException {
		Instant receivedAt = Instant.now();
		Capture capture = drainAndCapture(request);

		store.record(
				request.getMethod(),
				request.getRequestURI(),
				request.getQueryString(),
				request.getRemoteAddr(),
				request.getContentType(),
				collectHeaders(request),
				capture.bytesReceived(),
				capture.body(),
				capture.truncated(),
				receivedAt);

		ReceiptResponse receipt = new ReceiptResponse(
				"received",
				request.getMethod(),
				request.getRequestURI(),
				request.getContentType(),
				capture.bytesReceived(),
				receivedAt);
		return ResponseEntity.ok(receipt);
	}

	/**
	 * Reads the whole request body off the input stream. Bytes are counted; only
	 * the first {@link #BODY_CAPTURE_LIMIT} bytes are retained for the UI and the
	 * rest are discarded into the reused buffer.
	 */
	private Capture drainAndCapture(HttpServletRequest request) throws IOException {
		byte[] buffer = DRAIN_BUFFER.get();
		ByteArrayOutputStream retained = new ByteArrayOutputStream();
		long total = 0;

		try (InputStream in = request.getInputStream()) {
			int read;
			while ((read = in.read(buffer)) != -1) {
				total += read;
				int room = BODY_CAPTURE_LIMIT - retained.size();
				if (room > 0) {
					retained.write(buffer, 0, Math.min(read, room));
				}
				// Bytes beyond the capture limit are intentionally dropped.
			}
		}

		String body = retained.toString(StandardCharsets.UTF_8);
		return new Capture(total, body, total > retained.size());
	}

	private record Capture(long bytesReceived, String body, boolean truncated) {
	}

	private Map<String, String> collectHeaders(HttpServletRequest request) {
		Map<String, String> headers = new LinkedHashMap<>();
		Enumeration<String> names = request.getHeaderNames();
		while (names != null && names.hasMoreElements()) {
			String name = names.nextElement();
			List<String> values = new ArrayList<>();
			Enumeration<String> headerValues = request.getHeaders(name);
			while (headerValues.hasMoreElements()) {
				values.add(headerValues.nextElement());
			}
			headers.put(name, String.join(", ", values));
		}
		return Collections.unmodifiableMap(headers);
	}
}
