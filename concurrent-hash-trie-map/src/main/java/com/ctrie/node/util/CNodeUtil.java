package com.ctrie.node.util;

import com.ctrie.node.BasicNode;
import com.ctrie.node.CompressedNode;
import com.ctrie.node.Generation;
import com.ctrie.node.IndirectionNode;
import com.ctrie.node.ListNode;
import com.ctrie.node.MainNode;
import com.ctrie.node.SingletonNode;

import java.util.Map;

/**
 * Utility class for operations related to CNode.
 */
public final class CNodeUtil {

    // Maximum level in the trie. This value ensures that we don't exceed the maximum bit length of an integer hash code.
    private static final int MAX_LEVEL = 35;

    // Bit mask used for extracting 5 bits from a hash code, allowing us to use these bits as an index.
    private static final int BIT_MASK = 0x1f; // 0x1f is 31 in decimal, which is 11111 in binary.

    private CNodeUtil() {
        // Prevent instantiation
    }

    /**
     * Creates a dual-node structure from two singleton nodes.
     *
     * This method combines two singleton nodes into a more complex node structure, ensuring
     * that they fit into the correct position in the trie based on their hash codes.
     *
     * @param <K>        the type of keys
     * @param <V>        the type of values
     * @param nodeX      the first singleton node
     * @param hashX      the hash code of the first node
     * @param nodeY      the second singleton node
     * @param hashY      the hash code of the second node
     * @param level      the current level in the trie
     * @param generation the generation object
     * @return a new MainNode representing the combined structure
     */
    public static <K, V> MainNode<K, V> createDualNode(SingletonNode<K, V> nodeX, int hashX, SingletonNode<K, V> nodeY, int hashY, int level, Generation generation) {
        // Check if we are within the maximum allowed level of the trie.
        if (level < MAX_LEVEL) {
            // Extract 5 bits from the hash code to determine the position within the current level.
            int indexX = (hashX >>> level) & BIT_MASK;
            int indexY = (hashY >>> level) & BIT_MASK;

            // Create a bitmap with positions of both nodes.
            int bitmap = (1 << indexX) | (1 << indexY);

            // Check if both nodes should be placed in the same position.
            if (indexX == indexY) {
                // Both nodes fall in the same slot, so we need to create an internal node (INode).
                IndirectionNode<K, V> subINode = new IndirectionNode<>(generation);
                subINode.mainNode = createDualNode(nodeX, hashX, nodeY, hashY, level + 5, generation);
                return new CompressedNode<>(bitmap, new BasicNode[]{subINode}, generation);
            } else {
                // Nodes fall in different slots, so we create a CNode that contains both nodes.
                BasicNode[] nodesArray = (indexX < indexY)
                        ? new BasicNode[]{nodeX, nodeY}
                        : new BasicNode[]{nodeY, nodeX};
                return new CompressedNode<>(bitmap, nodesArray, generation);
            }
        } else {
            // If the level exceeds the maximum, we convert the structure to an LNode.
            Map<K,V> values = Map.of(nodeX.getKeyValuePair().getKey(), nodeX.getKeyValuePair().getValue(), nodeY.getKeyValuePair().getKey(), nodeY.getKeyValuePair().getValue());
            return new ListNode<>(values);
        }
    }

    /**
     * Detailed example of bitmap and indexing:
     *
     * Suppose we have two nodes with the following hash codes:
     * nodeX: 0b110010010011 (binary) = 3219 (decimal)
     * nodeY: 0b101001010010 (binary) = 2642 (decimal)
     *
     * We start at level 0:
     *
     * Step 1: Extract 5 bits from the hash code.
     * Level 0 (bits 0-4):
     * hashX: 0b10011 (binary) = 19 (decimal)
     * hashY: 0b10010 (binary) = 18 (decimal)
     *
     * Step 2: Create bitmap.
     * Bitmap: (1 << 19) | (1 << 18) = 0b1100000000000000000 (binary) = 786432 (decimal)
     *
     * Step 3: Check positions.
     * indexX and indexY are different, so we create a CNode with both nodes.
     *
     * Now, suppose both hash codes fall into the same slot at level 0:
     * hashX: 0b10000 (binary) = 16 (decimal)
     * hashY: 0b10000 (binary) = 16 (decimal)
     *
     * Bitmap: (1 << 16) = 0b10000000000000000 (binary) = 65536 (decimal)
     *
     * Since indexX == indexY, we create an internal node (INode) and recurse deeper.
     */
}
