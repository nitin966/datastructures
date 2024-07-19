package com.ctrie;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

abstract class MainNode<K,V> extends BasicNode {

    // static updater to update value of "prev".
    public static final AtomicReferenceFieldUpdater<MainNode, MainNode> updater = AtomicReferenceFieldUpdater.newUpdater(MainNode.class, MainNode.class, "prev");
    public volatile MainNode<K, V> prev = null;

    public abstract int cachedSize(Object ct);

    public boolean CAS_PREV(MainNode<K, V> oldVal, MainNode<K, V> newVal) {
        return updater.compareAndSet(this, oldVal, newVal);
    }

    public void WRITE_PREV(MainNode<K, V> nval) {
        updater.set(this, nval);
    }

    public MainNode<K, V> READ_PREV() {
        return updater.get(this);
    }
}
