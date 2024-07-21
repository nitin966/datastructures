package com.ctrie.node;

/**
 * Abstract class representing a basic node in the Concurrent Trie (Ctrie) structure.
 * All nodes in the trie should extend this class.
 */
public abstract class BasicNode {

    /**
     * Returns a string representation of the node, indented to reflect its level in the trie.
     *
     * @param level the level of the node in the trie
     * @return a string representation of the node
     */
    public abstract String toString(int level);
}
