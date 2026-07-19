package com.example.webhook;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Receiver endpoint. Accepts any HTTP method under {@code /webhook/**} - the
 * role SAP BTP Integration Suite calls - and returns 200 with a small receipt.
 *
 * <p><b>Memory note:</b> the request body is drained straight off the servlet
 * {@link InputStream} into a per-thread reusable buffer and discarded. Nothing
 * proportional to the body size is allocated on the heap, so large payloads do
 * not trigger GC pressure. Only the byte count is reported.
 */
@RestController
public class WebhookController {

	/** Size of the reusable drain buffer, per request thread. */
	private static final int BUFFER_SIZE = 8 * 1024;

	/**
	 * One 8 KB buffer per request thread, reused across requests. Tomcat's thread
	 * pool is bounded, so drain-buffer allocation is capped at (poolSize * 8 KB)
	 * for the whole application instead of allocating per request.
	 */
	private static final ThreadLocal<byte[]> DRAIN_BUFFER =
			ThreadLocal.withInitial(() -> new byte[BUFFER_SIZE]);

	@RequestMapping("/webhook/**")
	public ResponseEntity<ReceiptResponse> receive(HttpServletRequest request) throws IOException {
		long bytesReceived = drainBody(request);
		ReceiptResponse receipt = new ReceiptResponse(
				"received",
				request.getMethod(),
				request.getRequestURI(),
				request.getContentType(),
				bytesReceived,
				Instant.now());
		return ResponseEntity.ok(receipt);
	}

	/**
	 * Reads the whole request body off the input stream without ever holding it
	 * in memory. Bytes are counted and discarded into the reused buffer.
	 */
	private long drainBody(HttpServletRequest request) throws IOException {
		byte[] buffer = DRAIN_BUFFER.get();
		long total = 0;
		try (InputStream in = request.getInputStream()) {
			int read;
			while ((read = in.read(buffer)) != -1) {
				total += read;
			}
		}
		return total;
	}
}
