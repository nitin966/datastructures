package com.ctrie.node.util;

import com.ctrie.node.CompressedNode;
import com.ctrie.node.Generation;
import com.ctrie.node.BasicNode;
import com.ctrie.node.IndirectionNode;

/**
 * Utility class for operations related to INode.
 */
public final class INodeUtil {

    // Constants representing the presence or absence of a key.
    public static final Object KEY_PRESENT = new Object();
    public static final Object KEY_ABSENT = new Object();

    private INodeUtil() {
        // Prevent instantiation
    }

    /**
     * Creates a new root node for the trie.
     *
     * This method initializes a new generation and a new empty CNode,
     * then wraps them in an INode, which serves as the root of the trie.
     *
     * @param <K> the type of keys
     * @param <V> the type of values
     * @return a new INode representing the root node of the trie
     */
    public static <K, V> IndirectionNode<K, V> createNewRootNode() {
        // Initialize a new generation
        Generation generation = new Generation();

        // Create an empty CNode with a bitmap of 0 and an empty array of BasicNodes
        CompressedNode<K, V> emptyCNode = new CompressedNode<>(0, new BasicNode[0], generation);

        // Wrap the CNode in an INode and return it as the root node
        return new IndirectionNode<>(emptyCNode, generation);
    }
}
