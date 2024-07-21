package com.ctrie;

import com.ctrie.node.Generation;
import com.ctrie.node.IndirectionNode;
import com.ctrie.node.MainNode;
import com.ctrie.node.util.INodeUtil;
import com.ctrie.rdcss.RDCSS_Descriptor;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ConcurrentTrie<K, V> extends ConcurrentHashMap<K, V> {
    private AtomicReferenceFieldUpdater<ConcurrentTrie, Object> rootUpdater =
            AtomicReferenceFieldUpdater.newUpdater(ConcurrentTrie.class, Object.class, "root");

    private volatile Object root;

    public ConcurrentTrie() {
        this.root = INodeUtil.createNewRootNode();
    }

    private ConcurrentTrie(IndirectionNode<K, V> r, AtomicReferenceFieldUpdater<ConcurrentTrie, Object> rtupd) {
        this.root = r;
        this.rootUpdater = rtupd;
    }

    /* internal methods */

    private final boolean CAS_ROOT(Object ov, Object nv) {
        return rootUpdater.compareAndSet(this, ov, nv);
    }

    public final IndirectionNode<K, V> RDCSS_READ_ROOT(boolean abort) {
        Object r = root;
        if (r instanceof IndirectionNode<?,?>) {
            return (IndirectionNode<K, V>) r;
        } else {
            return RDCSS_Complete(abort);
        }
    }

    private IndirectionNode<K, V> RDCSS_Complete(boolean abort) {
        while (true) {
            Object v = root;
            if (v instanceof IndirectionNode) {
                return (IndirectionNode<K, V>) v;
            } else {
                RDCSS_Descriptor<K, V> desc = (RDCSS_Descriptor<K, V>) v;
                IndirectionNode<K, V> ov = desc.old;
                MainNode<K, V> exp = desc.expectedmain;
                IndirectionNode<K, V> nv = desc.nv;
                if (abort) {
                    if (CAS_ROOT(desc, ov)) return ov;
                } else {
                    MainNode<K, V> oldmain = ov.readCommittedMainNode(this);
                    if (oldmain == exp) {
                        if (CAS_ROOT(desc, nv)) {
                            desc.committed = true;
                            return nv;
                        }
                    } else {
                        if (CAS_ROOT(desc, ov)) return ov;
                    }
                }
            }
        }
    }

    private boolean RDCSS_ROOT(IndirectionNode<K, V> ov, MainNode<K, V> expectedmain, IndirectionNode<K, V> nv) {
        RDCSS_Descriptor<K, V> desc = new RDCSS_Descriptor<>(ov, expectedmain, nv);
        if (CAS_ROOT(ov, desc)) {
            RDCSS_Complete(false);
            return desc.committed;
        } else {
            return false;
        }
    }

    private void inserthc(K k, int hc, V v) {
        while (true) {
            IndirectionNode<K, V> r = RDCSS_READ_ROOT(false);
            if (!r.recInsert(k, v, hc, 0, null, r.getGen(), this)) continue;
            return;
        }
    }

    private Optional<V> insertifhc(K k, int hc, V v, Object cond) {
        while (true) {
            IndirectionNode<K, V> r = RDCSS_READ_ROOT(false);
            Optional<V> ret = r.recInsertIf(k, v, hc, cond, 0, null, r.getGen(), this);
            if (ret == null) continue;
            return ret;
        }
    }

    private Object lookuphc(K k, int hc) {
        while (true) {
            IndirectionNode<K, V> r = RDCSS_READ_ROOT(false);
            Object res = r.recLookup(k, hc, 0, null, r.getGen(), this);
            if (res == IndirectionNode.RESTART) continue;
            return res;
        }
    }

    private Optional<V> removehc(K k, V v, int hc) {
        while (true) {
            IndirectionNode<K, V> r = RDCSS_READ_ROOT(false);
            Optional<V> res = r.recRemove(k, v, hc, 0, null, r.getGen(), this);
            if (res.isEmpty()) continue;
            return res;
        }
    }

    public String string() {
        return RDCSS_READ_ROOT(false).string(0);
    }

    /* public methods */

    public final boolean isReadOnly() {
        return rootUpdater == null;
    }

    public final boolean nonReadOnly() {
        return rootUpdater != null;
    }

    public final ConcurrentTrie<K, V> snapshot() {
        while (true) {
            IndirectionNode<K, V> r = RDCSS_READ_ROOT(false);
            MainNode<K, V> expmain = r.readCommittedMainNode(this);
            if (RDCSS_ROOT(r, expmain, r.copyToGen(new Generation(), this))) {
                return new ConcurrentTrie<>(r.copyToGen(new Generation(), this), rootUpdater);
            }
        }
    }

    public final ConcurrentTrie<K, V> readOnlySnapshot() {
        while (true) {
            IndirectionNode<K, V> r = RDCSS_READ_ROOT(false);
            MainNode<K, V> expmain = r.readCommittedMainNode(this);
            if (RDCSS_ROOT(r, expmain, r.copyToGen(new Generation(), this))) {
                return new ConcurrentTrie<>(r, null);
            }
        }
    }

    @Override
    public final void clear() {
        while (true) {
            IndirectionNode<K, V> r = RDCSS_READ_ROOT(false);
            if (RDCSS_ROOT(r, r.readCommittedMainNode(this), INodeUtil.createNewRootNode())) {
                return;
            }
        }
    }

    public final V lookup(K k) {
        int hc = ConcurrentTrieUtil.computeHash(k);
        return (V) lookuphc(k, hc);
    }

    @Override
    public final V get(Object k) {
        int hc = ConcurrentTrieUtil.computeHash((K) k);
        Object res = lookuphc((K) k, hc);
        if (res == null) throw new NoSuchElementException();
        else return (V) res;
    }

    @Override
    public final V put(K key, V value) {
        int hc = ConcurrentTrieUtil.computeHash(key);
        Optional<V> result = insertifhc(key, hc, value, null);
        return result.orElse(null);
    }

    @Override
    public final V remove(Object k) {
        int hc = ConcurrentTrieUtil.computeHash((K) k);
        Optional<V> result = removehc((K) k, null, hc);
        return result.orElse(null);
    }

    @Override
    public final boolean containsKey(Object k) {
        int hc = ConcurrentTrieUtil.computeHash((K) k);
        return lookuphc((K) k, hc) != null;
    }

    @Override
    public final boolean containsValue(Object value) {
        // You may need to iterate through the entire trie to check for a value.
        throw new UnsupportedOperationException("containsValue is not supported.");
    }

    @Override
    public final V putIfAbsent(K k, V v) {
        int hc = ConcurrentTrieUtil.computeHash(k);
        Optional<V> result = insertifhc(k, hc, v, INodeUtil.KEY_ABSENT);
        return result.orElse(null);
    }

    @Override
    public final boolean remove(Object k, Object v) {
        int hc = ConcurrentTrieUtil.computeHash((K) k);
        return removehc((K) k, (V) v, hc).isPresent();
    }

    @Override
    public final boolean replace(K k, V oldValue, V newValue) {
        int hc = ConcurrentTrieUtil.computeHash(k);
        return insertifhc(k, hc, newValue, oldValue).isPresent();
    }

    @Override
    public final V replace(K k, V v) {
        int hc = ConcurrentTrieUtil.computeHash(k);
        Optional<V> result = insertifhc(k, hc, v, INodeUtil.KEY_PRESENT);
        return result.orElse(null);
    }

    /*@Override
    public Iterator<Map.Entry<K, V>> iterator() {
        if (nonReadOnly()) return readOnlySnapshot().entrySet().iterator();
        else return new CtrieIterator<>(this);
    }*/

    // Other methods required by the ConcurrentMap interface that are not overridden here can throw UnsupportedOperationException or can be implemented similarly.
    // Methods like size(), isEmpty(), entrySet(), keySet(), values(), etc., may need to be implemented depending on the full requirements.
}

