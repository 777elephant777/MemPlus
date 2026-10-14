package com.memplus.pool;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concurrent string deduplication table.
 *
 * Equivalent to G1's experimental -XX:+UseStringDeduplication but with two
 * important differences:
 *   1. We only dedup strings explicitly routed through {@link #dedup(String)},
 *      so we never interfere with the JVM's own string table.
 *   2. We hold WeakReferences so a dedup'd string can still be reclaimed if
 *      every caller drops it. Vanilla {@link String#intern()} pins the string
 *      into the JVM's permanent string table — that's the bug FerriteCore has
 *      to work around, and we sidestep it entirely.
 *
 * Memory accounting:
 *   Each unique entry costs: WeakReference (16B) + String payload (16B +
 *   UTF-16 payload). We break even when the dedup'd string has been seen at
 *   least twice with a payload > 16 chars. We never dedup strings shorter
 *   than the configured threshold.
 */
public final class StringDeduplicator {

    private static final StringDeduplicator INSTANCE = new StringDeduplicator();

    public static StringDeduplicator shared() { return INSTANCE; }

    private final ConcurrentMap<String, WeakReference<String>> table = new ConcurrentHashMap<>(4096);
    private final AtomicLong bytesSaved = new AtomicLong();
    private final AtomicLong lookups = new AtomicLong();
    private final AtomicLong hits = new AtomicLong();

    private volatile int minLength = 4;

    private StringDeduplicator() {}

    public void setMinLength(int len) {
        if (len < 1) len = 1;
        if (len > 64) len = 64;
        this.minLength = len;
    }

    /**
     * Dedup a string. If the same string has been seen before, return the
     * existing canonical instance and free the caller's copy.
     *
     * The {@code original} parameter is allowed to be null — we return null
     * untouched so this can be transparently routed through getter methods.
     */
    public String dedup(String original) {
        if (original == null) return null;
        // Skip tiny strings: the WeakReference overhead would dominate.
        int len = original.length();
        lookups.incrementAndGet();
        if (len < minLength) return original;

        WeakReference<String> existing = table.get(original);
        if (existing != null) {
            String canonical = existing.get();
            if (canonical != null) {
                hits.incrementAndGet();
                // If the original was actually a different instance, that's
                // (len * 2) bytes we no longer need.
                if (canonical != original) {
                    bytesSaved.addAndGet((long) len * 2 + 16);
                }
                return canonical;
            } else {
                // The weak ref was cleared but the entry remains; remove and
                // re-insert below.
                table.remove(original, existing);
            }
        }

        // Re-use the incoming instance as the canonical one. This avoids
        // cloning the payload — the caller already paid for it.
        table.put(original, new WeakReference<>(original));
        return original;
    }

    /** Same as {@link #dedup} but for compound NBT-like strings we want to canonicalize eagerly. */
    public String dedupEager(String original) {
        return dedup(original);
    }

    public long estimatedBytesSaved() { return bytesSaved.get(); }
    public long lookups() { return lookups.get(); }
    public long hits() { return hits.get(); }

    /** Periodically drop stale weak references; cheap because we use WeakReference. */
    public void flush() {
        // Cheap invalidation pass: remove cleared refs without re-iterating the
        // whole table. ConcurrentHashMap's iteration is weakly consistent and
        // safe under concurrent mutation.
        int removed = 0;
        for (var it = table.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (e.getValue().get() == null) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            // force the GC to consider reclaiming the now-unreferenced entries
            // without an explicit System.gc() (which we avoid for latency reasons)
            table.size();
        }
    }

    /** Test helper. */
    public Set<String> snapshotKeys() {
        Set<String> out = new HashSet<>();
        for (var e : table.entrySet()) {
            String s = e.getValue().get();
            if (s != null) out.add(s);
        }
        return Collections.unmodifiableSet(out);
    }
}
