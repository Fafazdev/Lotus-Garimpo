package lotus.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<String, long[]> tracker = new ConcurrentHashMap<>();

    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();
        long[] entry = tracker.compute(key, (k, v) -> {
            if (v == null || now - v[0] > WINDOW_MILLIS) {
                return new long[]{now, 1};
            }
            v[1]++;
            return v;
        });
        return entry[1] <= MAX_ATTEMPTS;
    }
}
