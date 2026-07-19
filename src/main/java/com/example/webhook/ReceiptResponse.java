package com.example.webhook;

import java.time.Instant;

/**
 * Receipt returned to the sender (SAP BTP Integration Suite) confirming that the
 * request was received. The body is drained off the input stream and only its
 * byte count is reported - it is never held whole in memory.
 */
public record ReceiptResponse(
		String status,
		String method,
		String path,
		String contentType,
		long bytesReceived,
		Instant receivedAt) {
}
