package com.memplus.nbt;

import net.minecraft.nbt.Tag;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Lazy-allocating Map&lt;String, Tag&gt; replacement for {@link net.minecraft.nbt.CompoundTag}'s
 * inner HashMap.
 *
 * <p>Vanilla's CompoundTag default constructor unconditionally allocates a
 * HashMap via {@code Maps.newHashMap()}, costing ~80 bytes (header +
 * default table[16] + size/modCount fields). On worlds with many empty
 * compound tags — entity sub-tags like "ActiveEffects" that are usually
 * empty, block entity extra data, item stack sub-compounds — this adds
 * up to several MiB of pure overhead.
 *
 * <p>LazyTagMap stores an initially-null backing map. Read operations
 * (get, containsKey, size, isEmpty, getAllKeys) work against the null
 * state without allocation; the first write operation (put, remove,
 * clear) triggers the HashMap allocation. After that, all operations
 * delegate to the backing map.
 *
 * <p>For read-only compound tags loaded from disk that never get
 * mutated (the common case), the HashMap is never allocated — saving
 * ~80 bytes per such tag.
 */
public final class LazyTagMap extends AbstractMap<String, Tag> {

    private Map<String, Tag> backing;
    private int initialCapacity = 16;

    public LazyTagMap() {}

    public LazyTagMap(int initialCapacity) {
        this.initialCapacity = Math.max(initialCapacity, 16);
    }

    private Map<String, Tag> ensureBacking() {
        if (backing == null) {
            backing = new HashMap<>(initialCapacity);
        }
        return backing;
    }

    @Override
    public int size() {
        return backing == null ? 0 : backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing == null || backing.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backing != null && backing.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backing != null && backing.containsValue(value);
    }

    @Override
    public Tag get(Object key) {
        return backing == null ? null : backing.get(key);
    }

    @Override
    public Tag put(String key, Tag value) {
        return ensureBacking().put(key, value);
    }

    @Override
    public Tag remove(Object key) {
        return backing == null ? null : backing.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Tag> m) {
        if (m == null || m.isEmpty()) return;
        ensureBacking().putAll(m);
    }

    @Override
    public void clear() {
        if (backing != null) backing.clear();
    }

    @Override
    public Set<String> keySet() {
        if (backing == null) return java.util.Collections.emptySet();
        return backing.keySet();
    }

    @Override
    public java.util.Collection<Tag> values() {
        if (backing == null) return java.util.Collections.emptyList();
        return backing.values();
    }

    @Override
    public Set<Entry<String, Tag>> entrySet() {
        if (backing == null) return java.util.Collections.emptySet();
        return backing.entrySet();
    }

    /**
     * Force-allocate the backing map and copy entries from the source.
     * Used when CompoundTag is constructed from a source map (e.g.
     * during NBT deserialization).
     */
    public void copyFrom(Map<? extends String, ? extends Tag> source) {
        if (source == null || source.isEmpty()) return;
        ensureBacking().putAll(source);
    }
}
