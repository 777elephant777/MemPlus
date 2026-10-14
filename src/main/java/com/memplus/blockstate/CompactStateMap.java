package com.memplus.blockstate;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

/**
 * Capacity-tight, frozen-after-construction replacement for the per-state
 * {@code Reference2ObjectArrayMap<Property<?>, Comparable<?>>} used as
 * StateHolder's values field.
 *
 * <p>The {@code values} field in StateHolder is typed as
 * {@link Reference2ObjectArrayMap}, so we must extend that class.
 * The savings we can get:
 *
 * <ul>
 *   <li>Use exact-capacity constructors (no growth slack on the key/value
 *       arrays — vanilla's default-constructor version allocates a
 *       size-16 array even for 1-2 properties).
 *   <li>Override {@link #put}, {@link #remove}, {@link #clear} to throw
 *       UnsupportedOperationException. This both catches mutation bugs
 *       early AND lets the JIT inline the no-op branch for callers that
 *       happen to check isFrozen before mutating.
 * </ul>
 *
 * <p>Savings: ~16 bytes per state from dropping the default-capacity
 * slack on the backing arrays. On a 200k-state modpack that's ~3 MiB
 * freed just from this — modest, but every bit helps.
 */
public final class CompactStateMap<K, V> extends Reference2ObjectArrayMap<K, V> {

    public CompactStateMap() {
        super(0);
    }

    public CompactStateMap(Map<? extends K, ? extends V> source) {
        // Reference2ObjectArrayMap(Map) constructor directly populates the
        // internal key[] and value[] arrays via getfield/putfield, NOT via
        // put(). So our overridden put() throwing UnsupportedOperationException
        // does not interfere with this constructor.
        super(source);
    }

    public CompactStateMap(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public V put(K key, V value) {
        // StateHolder's values are immutable after construction; surface
        // mutation attempts as bugs rather than silently corrupting state.
        throw new UnsupportedOperationException(
                "CompactStateMap is frozen (state values are immutable)");
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("CompactStateMap is frozen");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("CompactStateMap is frozen");
    }

    /**
     * Build a frozen compact map from a source map.
     */
    public static <K, V> CompactStateMap<K, V> from(Reference2ObjectMap<K, V> source) {
        // The Map-based constructor copies entries without going through
        // our overridden put(), so this works even with the frozen policy.
        return new CompactStateMap<>(source);
    }
}
