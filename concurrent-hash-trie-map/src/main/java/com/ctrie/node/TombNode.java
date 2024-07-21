package com.ctrie.node;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Represents a tombed node in the Ctrie structure.
 * A tombed node indicates that the node has been logically removed but may still be
 * present in the structure for synchronization purposes.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 */
public final class TombNode<K, V> extends MainNode<K, V> implements KeyValueNode<K, V> {
    private final K key;
    private final V value;
    private final int hash;

    /**
     * Constructs a TombNode with the given key, value, and hash.
     *
     * @param key   the key stored in this node
     * @param value the value stored in this node
     * @param hash  the hash code of the key
     */
    public TombNode(K key, V value, int hash) {
        this.key = key;
        this.value = value;
        this.hash = hash;
    }

    /**
     * Creates a copy of this tombed node.
     *
     * @return a new TombNode with the same key, value, and hash
     */
    public TombNode<K, V> copy() {
        return new TombNode<>(key, value, hash);
    }

    /**
     * Creates a tombed copy of this node.
     *
     * @return a new TombNode with the same key, value, and hash
     */
    public TombNode<K, V> copyTombed() {
        return new TombNode<>(key, value, hash);
    }

    /**
     * Creates an untombed copy of this node.
     * This is used to revert a tombed node back to its original state.
     *
     * @return a new SingletonNode with the same key, value, and hash
     */
    public SingletonNode<K, V> copyUntombed() {
        return new SingletonNode<>(key, value, hash);
    }

    @Override
    public Map.Entry<K, V> getKeyValuePair() {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public String toString(int level) {
        return " ".repeat(level) + "TombNode(" + key + ", " + value + ", " + Integer.toHexString(hash) + ", !)";
    }
}
