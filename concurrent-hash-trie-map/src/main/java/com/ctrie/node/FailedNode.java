package com.ctrie.node;

/**
 * A node that represents a failed operation in the concurrent trie.
 * This is used to indicate that an operation could not be completed successfully.
 *
 * @param <K>> The type of keys stored in the trie.
 * @param <V>> The type of values stored in the trie.
 */
public final class FailedNode<K, V> extends MainNode<K, V> {

    /**
     * Constructs a FailedNode with the given previous node.
     *
     * @param previous the previous main node
     */
    public FailedNode(MainNode<K, V> previous) {
        WRITE_PREV(previous);
    }

    @Override
    public String toString(int level) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "FailedNode(" + prev + ")";
    }
}
