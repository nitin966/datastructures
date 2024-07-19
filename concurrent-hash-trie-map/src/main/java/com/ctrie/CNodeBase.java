package com.ctrie;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

abstract class CNodeBase<K,V> extends MainNode<K,V> {

    @SuppressWarnings("rawtypes")
    public static final AtomicIntegerFieldUpdater<CNodeBase> updater = AtomicIntegerFieldUpdater.newUpdater(CNodeBase.class, "csize");
    public volatile int csize = -1;

    public boolean CAS_SIZE(int oldVal, int newVal) {
        return updater.compareAndSet(this, oldVal, newVal);
    }

    public void WRITE_SIZE(int newVal) {
        updater.set(this, newVal);
    }

    public int READ_SIZE() {
        return updater.get(this);
    }
}
