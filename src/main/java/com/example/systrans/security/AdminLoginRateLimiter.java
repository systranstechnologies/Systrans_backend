package com.example.systrans.security;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class AdminLoginRateLimiter {
	private static final int MAX_ATTEMPTS = 10;
	private static final long WINDOW_MILLIS = Duration.ofMinutes(15).toMillis();
	private final ConcurrentHashMap<String, ArrayDeque<Long>> attempts = new ConcurrentHashMap<>();

	public synchronized boolean allowAttempt(String key) {
		long now = System.currentTimeMillis();
		ArrayDeque<Long> recentAttempts = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
		while (!recentAttempts.isEmpty() && now - recentAttempts.peekFirst() >= WINDOW_MILLIS) {
			recentAttempts.removeFirst();
		}
		if (recentAttempts.size() >= MAX_ATTEMPTS) {
			return false;
		}
		recentAttempts.addLast(now);
		return true;
	}

	public synchronized void clear(String key) {
		attempts.remove(key);
	}
}
