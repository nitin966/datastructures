package com.ctrie.node;

import com.ctrie.ConcurrentTrie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A compressed node that holds an array of sub-nodes in the concurrent trie.
 * This class uses a bitmap to indicate which positions are occupied and helps
 * to reduce memory footprint.
 *
 * <p>In a compressed node, only the positions specified by the bitmap are
 * occupied by sub-nodes. This compression reduces memory usage by avoiding
 * empty slots in sparsely populated nodes.</p>
 *
 * <p><strong>Example:</strong></p>
 * <p>Suppose we have a bitmap of 10101 (binary) which corresponds to 21 (decimal).
 * This means that the sub-nodes are present at positions 0, 2, and 4.</p>
 * <pre>
 *     CompressedNode<Integer, String> node = new CompressedNode<>(0b10101, new TrieNode[]{
 *         new KeyValueNode<>(1, "one"),
 *         new KeyValueNode<>(3, "three"),
 *         new KeyValueNode<>(5, "five")
 *     }, new Generation());
 * </pre>
 *
 * @param <K> The type of keys stored in the trie.
 * @param <V> The type of values stored in the trie.
 */
public final class CompressedNode<K, V> extends MainNode<K, V> {
    /** The bitmap indicating which positions are occupied. */
    public final int bitmap;
    /** The array of sub-nodes. */
    public final BasicNode[] array;
    /** The generation of the trie. */
    public final Generation generation;

    /**
     * Constructs a new CompressedNode with the given bitmap, array of sub-nodes, and generation.
     *
     * @param bitmap The bitmap indicating which positions are occupied.
     * @param array The array of sub-nodes.
     * @param generation The generation of the trie.
     */
    public CompressedNode(int bitmap, BasicNode[] array, Generation generation) {
        this.bitmap = bitmap;
        this.array = array;
        this.generation = generation;
    }

    /**
     * Returns a copy of this CNode with an updated node at the specified position.
     *
     * @param pos the position to update
     * @param newNode the new node to insert
     * @param newGen the new generation
     * @return a new CNode with the updated node
     */
    public CompressedNode<K, V> updatedAt(int pos, BasicNode newNode, Generation newGen) {
        BasicNode[] newArray = array.clone();
        newArray[pos] = newNode;
        return new CompressedNode<>(bitmap, newArray, newGen);
    }

    /**
     * Returns a copy of this CNode with a node removed from the specified position.
     *
     * @param pos the position to remove
     * @param flag the flag indicating the bit to be cleared in the bitmap
     * @param newGen the new generation
     * @return a new CNode with the node removed
     */
    public CompressedNode<K, V> removedAt(int pos, int flag, Generation newGen) {
        BasicNode[] newArray = new BasicNode[array.length - 1];
        System.arraycopy(array, 0, newArray, 0, pos);
        System.arraycopy(array, pos + 1, newArray, pos, array.length - pos - 1);
        return new CompressedNode<>(bitmap ^ flag, newArray, newGen);
    }

    /**
     * Returns a copy of this CNode with a new node inserted at the specified position.
     *
     * @param pos the position to insert
     * @param flag the flag indicating the bit to be set in the bitmap
     * @param newNode the new node to insert
     * @param newGen the new generation
     * @return a new CNode with the node inserted
     */
    public CompressedNode<K, V> insertedAt(int pos, int flag, BasicNode newNode, Generation newGen) {
        BasicNode[] newArray = new BasicNode[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, pos);
        newArray[pos] = newNode;
        System.arraycopy(array, pos, newArray, pos + 1, array.length - pos);
        return new CompressedNode<>(bitmap | flag, newArray, newGen);
    }

    /**
     * Returns a copy of this CNode with all INode instances copied to the specified generation.
     *
     * @param newGen the new generation
     * @param ct the concurrent trie
     * @return a new CNode with updated generation
     */
    public CompressedNode<K, V> renewed(Generation newGen, ConcurrentTrie<K, V> ct) {
        BasicNode[] newArray = new BasicNode[array.length];
        for (int i = 0; i < array.length; i++) {
            if (array[i] instanceof IndirectionNode) {
                newArray[i] = ((IndirectionNode<K, V>) array[i]).copyToGen(newGen, ct);
            } else {
                newArray[i] = array[i];
            }
        }
        return new CompressedNode<>(bitmap, newArray, newGen);
    }

    /**
     * Resurrects an INode by returning its untombed version if it is a tombed TNode,
     * otherwise returns the original INode.
     *
     * @param inode the INode to resurrect
     * @param inodemain the main node of the INode
     * @return the resurrected BasicNode
     */
    private BasicNode resurrect(IndirectionNode<K, V> inode, Object inodemain) {
        if (inodemain instanceof TombNode) {
            return ((TombNode<K, V>) inodemain).copyUntombed();
        } else {
            return inode;
        }
    }

    /**
     * Converts this CNode to a contracted version if it contains only one child.
     *
     * @param level the level in the trie
     * @return the contracted node
     */
    public BasicNode toContracted(int level) {
        if (array.length == 1 && level > 0) {
            if (array[0] instanceof SingletonNode<?,?>) {
                return ((SingletonNode<K, V>) array[0]).copyTombed();
            }
        }
        return this;
    }

    /**
     * Compresses this CNode by removing null INodes and returns the compressed version.
     *
     * @param ct the concurrent trie
     * @param level the level in the trie
     * @param gen the generation
     * @return the compressed node
     */
    public BasicNode toCompressed(ConcurrentTrie<K, V> ct, int level, Generation gen) {
        BasicNode[] tempArray = new BasicNode[array.length];
        for (int i = 0; i < array.length; i++) {
            BasicNode subNode = array[i];
            if (subNode instanceof IndirectionNode<?,?>) {
                Object mainNode = ((IndirectionNode<K, V>) subNode).readCommittedMainNode(ct);
                assert mainNode != null;
                tempArray[i] = resurrect((IndirectionNode<K, V>) subNode, mainNode);
            } else if (subNode instanceof SingletonNode<?,?>) {
                tempArray[i] = (SingletonNode<K, V>) subNode;
            }
        }
        return new CompressedNode<>(bitmap, tempArray, gen).toContracted(level);
    }

    /**
     * Returns a string representation of the CNode.
     *
     * @param level the level in the trie
     * @return a string representation of the CNode
     */
    public String string(int level) {
        String indent = " ".repeat(level);
        return String.format("CNode %x\n%s", bitmap, Arrays.stream(array)
                .map(bn -> indent + bn.toString(level + 1))
                .collect(Collectors.joining("\n")));
    }

    /**
     * Collects all key-value pairs from the CNode and its children.
     * This method is quiescently consistent and should not be called concurrently with GCAS operations.
     *
     * @return a list of key-value pairs
     */
    private List<Map.Entry<K, V>> collectElems() {
        List<Map.Entry<K, V>> elements = new ArrayList<>();
        for (BasicNode bn : array) {
            if (bn instanceof SingletonNode<?,?>) {
                elements.add(((SingletonNode<K, V>) bn).getKeyValuePair());
            } else if (bn instanceof IndirectionNode) {
                MainNode<K, V> mainNode = ((IndirectionNode<K, V>) bn).mainNode;
                if (mainNode instanceof TombNode<?,?>) {
                    elements.add(((TombNode<K, V>) mainNode).getKeyValuePair());
                } else if (mainNode instanceof ListNode) {
                    elements.addAll(((ListNode<K, V>) mainNode).listMap.entrySet());
                } else if (mainNode instanceof CompressedNode) {
                    elements.addAll(((CompressedNode<K, V>) mainNode).collectElems());
                }
            }
        }
        return elements;
    }

    /**
     * Collects local key-value pairs from the CNode without traversing further into the trie.
     *
     * @return a list of local key-value pairs as strings
     */
    private List<String> collectLocalElems() {
        List<String> elements = new ArrayList<>();
        for (BasicNode bn : array) {
            if (bn instanceof SingletonNode<?,?>) {
                elements.add(((SingletonNode<?,?>) bn).getValue().toString());
            } else if (bn instanceof IndirectionNode) {
                elements.add(((IndirectionNode<K, V>) bn).toString().substring(14) + "(" + ((IndirectionNode<K, V>) bn).getGen() + ")");
            }
        }
        return elements;
    }

    @Override
    public String toString() {
        List<String> elements = collectLocalElems();
        return String.format("CNode(sz: %d; %s)", elements.size(), String.join(", ", elements));
    }

    /**
     * Returns a string representation of the compressed node.
     *
     * @param level The level in the trie.
     * @return The string representation.
     */
    public String toString(int level) {
        String indent = "  ".repeat(level);
        String subNodesString = Arrays.stream(array)
                .map(subNode -> subNode.toString(level + 1))
                .collect(Collectors.joining("\n"));

        return String.format("%sCompressedNode %x\n%s", indent, bitmap, subNodesString);
    }

    static <K, V> MainNode<K,V> dual (final SingletonNode<K, V> x, int xhc, final SingletonNode<K, V> y, int yhc, int lev, Generation gen) {
        if (lev < 35) {
            int xidx = (xhc >>> lev) & 0x1f;
            int yidx = (yhc >>> lev) & 0x1f;
            int bmp = (1 << xidx) | (1 << yidx);

            if (xidx == yidx) {
                IndirectionNode<K, V> subinode = new IndirectionNode<>(gen);// (TrieMap.inodeupdater)
                subinode.mainNode = dual (x, xhc, y, yhc, lev + 5, gen);
                return new CompressedNode<>(bmp, new BasicNode[] { subinode }, gen);
            } else {
                if (xidx < yidx)
                    return new CompressedNode<>(bmp, new BasicNode[] { x, y }, gen);
                else
                    return new CompressedNode<>(bmp, new BasicNode[] { y, x }, gen);
            }
        } else {
            return new ListNode<>(Map.of(x.getKey(), x.getValue(), y.getKey(), y.getValue()));
        }
    }
}
