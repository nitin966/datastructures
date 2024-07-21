package com.ctrie.rdcss;

import com.ctrie.node.IndirectionNode;
import com.ctrie.node.MainNode;

public class RDCSS_Descriptor<K, V> {
    public final IndirectionNode<K, V> old;
    public final MainNode<K, V> expectedmain;
    public final IndirectionNode<K, V> nv;

    volatile public boolean committed;

    public RDCSS_Descriptor(IndirectionNode<K, V> old, MainNode<K, V> expectedmain, IndirectionNode<K, V> nv) {
        this.old = old;
        this.expectedmain = expectedmain;
        this.nv = nv;
        this.committed = false;
    }
}
