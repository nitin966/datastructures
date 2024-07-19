package com.ctrie;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

abstract class INodeBase<K,V> extends BasicNode {

    @SuppressWarnings("rawtypes")
    public static final AtomicReferenceFieldUpdater<INodeBase, MainNode> updater = AtomicReferenceFieldUpdater.newUpdater(INodeBase.class, MainNode.class, "mainnode");

    public static final Object RESTART = new Object();

    public volatile MainNode<K, V> mainnode = null;

    public final Gen gen;

    public INodeBase(Gen generation) {
        gen = generation;
    }

    public BasicNode prev() {
        return null;
    }
}
