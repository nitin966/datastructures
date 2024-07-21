package com.ctrie.node;

import com.ctrie.ConcurrentTrieUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A node that holds a list of key-value pairs in the concurrent trie.
 * This is used for handling hash collisions.
 *
 * @param <K> The type of keys stored in the trie.
 * @param <V> The type of values stored in the trie.
 */
public final class ListNode<K, V> extends MainNode<K, V> {

    /**
     * The map storing key-value pairs in this list node.
     */
    public final Map<K, V> listMap;

    /**
     * Constructs a ListNode with the given list of key-value pairs.
     *
     * @param listMap the map storing key-value pairs
     */
    public ListNode(Map<K, V> listMap) {
        this.listMap = listMap;
    }

    /**
     * Inserts a key-value pair into this node.
     *
     * @param key   the key to insert
     * @param value the value to insert
     * @return a new ListNode with the key-value pair inserted
     */
    public ListNode<K, V> inserted(K key, V value) {
        listMap.put(key, value);
        return new ListNode<>(listMap);
    }

    /**
     * Inserts two key-value pair into this node.
     *
     * @param key1   the key to insert
     * @param value1 the value to insert
     * @param key2   the key to insert
     * @param value2 the value to insert
     * @return a new ListNode with the key-value pair inserted
     */
    public ListNode<K, V> inserted(K key1, V value1, K key2, V value2) {
        ListNode<K, V> listNode = new ListNode<>(new HashMap<>());
        listMap.put(key1, value1);
        listMap.put(key2, value2);
        return new ListNode<>(listMap);
    }

    /**
     * Removes a key from this node.
     *
     * @param key the key to remove
     * @return a new ListNode with the key removed, or a tombed node if only one element remains
     */
    public MainNode<K, V> removed(K key) {
        Map<K, V> updatedMap = new HashMap<>(listMap);
        updatedMap.remove(key);

        if (updatedMap.size() > 1) {
            return new ListNode<>(updatedMap);
        } else {
            Iterator<Map.Entry<K, V>> iterator = updatedMap.entrySet().iterator();
            if (iterator.hasNext()) {
                Map.Entry<K, V> entry = iterator.next();
                return new TombNode<>(entry.getKey(), entry.getValue(), ConcurrentTrieUtil.computeHash(entry.getKey()));
            } else {
                // This case should ideally not occur since we check for size > 1 before
                throw new IllegalStateException("Updated map is unexpectedly empty.");
            }
        }
    }

    /**
     * Retrieves the value associated with a key in this node.
     *
     * @param key the key to look up
     * @return the value associated with the key, or null if not found
     */
    public V get(K key) {
        return listMap.get(key);
    }

    @Override
    public String toString(int level) {
        return " ".repeat(level) + "ListNode(" + listMap.toString() + ")";
    }
}
