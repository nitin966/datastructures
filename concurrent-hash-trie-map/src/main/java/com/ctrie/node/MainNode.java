package com.ctrie.node;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * The base class for nodes that hold actual data or other nodes in the concurrent trie.
 * This is an abstract class with several specialized subclasses.
 *
 * @param <K> The type of keys stored in the trie.
 * @param <V> The type of values stored in the trie.
 */
public abstract class MainNode<K,V> extends BasicNode {

    @SuppressWarnings("rawtypes")
    // static updater to update value of "prev".
    public static final AtomicReferenceFieldUpdater<MainNode, MainNode> updater = AtomicReferenceFieldUpdater.newUpdater(MainNode.class, MainNode.class, "prev");
    public volatile MainNode<K, V> prev = null;

    //public abstract int cachedSize(Object ct);

    public boolean CAS_PREV(MainNode<K, V> oldVal, MainNode<K, V> newVal) {
        return updater.compareAndSet(this, oldVal, newVal);
    }

    public void WRITE_PREV(MainNode<K, V> nval) {
        updater.set(this, nval);
    }
}
