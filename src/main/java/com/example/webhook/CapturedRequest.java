package com.example.webhook;

import java.time.Instant;
import java.util.Map;

/**
 * A single captured request, held in memory for display in the inspector UI.
 *
 * <p>The body is captured up to a bounded limit ({@code body} may be truncated,
 * as flagged by {@code bodyTruncated}); {@code bytesReceived} always reflects
 * the full length that was streamed in.
 */
public record CapturedRequest(
		long seq,
		String id,
		Instant receivedAt,
		String method,
		String path,
		String query,
		String remoteAddr,
		String contentType,
		Map<String, String> headers,
		long bytesReceived,
		String body,
		boolean bodyTruncated) {
}
