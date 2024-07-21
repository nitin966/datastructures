package com.ctrie.node;

import java.util.Map;

/**
 * A node that represents a key-value pair in the concurrent trie.
 *
 * <p>This node is used to store actual key-value pairs in the trie. It is a terminal
 * node, meaning it does not have any children.</p>
 *
 * @param <K> The type of keys stored in the trie.
 * @param <V> The type of values stored in the trie.
 */
public interface KeyValueNode<K, V> {
    /**
     * Returns the key-value pair stored in this node.
     *
     * @return the key-value pair
     */
    Map.Entry<K, V> getKeyValuePair();

    /** Returns the key associated with this node. */
    K getKey();

    /** Returns the value associated with this node. */
    V getValue();
}
