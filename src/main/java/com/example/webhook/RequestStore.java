package com.example.webhook;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * In-memory, bounded store of the most recent captured requests. Newest first.
 *
 * <p>Only the last {@link #MAX_REQUESTS} requests are kept; older ones are
 * dropped. Access is synchronized so many Tomcat worker threads can record
 * concurrently while the UI reads a consistent snapshot.
 */
@Component
public class RequestStore {

	/** How many recent requests to retain. */
	static final int MAX_REQUESTS = 200;

	private final Deque<CapturedRequest> requests = new ArrayDeque<>();
	private final AtomicLong sequence = new AtomicLong();

	public synchronized CapturedRequest record(
			String method,
			String path,
			String query,
			String remoteAddr,
			String contentType,
			Map<String, String> headers,
			long bytesReceived,
			String body,
			boolean bodyTruncated,
			Instant receivedAt) {

		CapturedRequest captured = new CapturedRequest(
				sequence.incrementAndGet(),
				UUID.randomUUID().toString(),
				receivedAt,
				method,
				path,
				query,
				remoteAddr,
				contentType,
				headers,
				bytesReceived,
				body,
				bodyTruncated);

		requests.addFirst(captured);
		while (requests.size() > MAX_REQUESTS) {
			requests.removeLast();
		}
		return captured;
	}

	/** Snapshot of captured requests, newest first. */
	public synchronized List<CapturedRequest> list() {
		return new ArrayList<>(requests);
	}

	public synchronized void clear() {
		requests.clear();
	}
}
