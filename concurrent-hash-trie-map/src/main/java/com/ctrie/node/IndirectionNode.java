package com.ctrie.node;

import com.ctrie.ConcurrentTrie;
import com.ctrie.node.util.CNodeUtil;
import com.ctrie.node.util.INodeUtil;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * An indirection node (INode) in the concurrent trie.
 * This class represents a node that points to the main node of a trie.
 * It provides methods to manage the node in a lock-free manner using CAS (Compare-And-Swap) operations.
 *
 * @param <K>   The type of keys stored in the trie.
 * @param <V> The type of values stored in the trie.
 */
public final class IndirectionNode<K,V> extends BasicNode {

    public static final Object RESTART = new Object();
    private static final Object KEY_PRESENT = new Object();
    private static final Object KEY_ABSENT = new Object();

    @SuppressWarnings("rawtypes")
    public static final AtomicReferenceFieldUpdater<IndirectionNode, MainNode> UPDATER = AtomicReferenceFieldUpdater.newUpdater(IndirectionNode.class, MainNode.class, "mainNode");

    public volatile MainNode<K, V> mainNode;
    private final Generation gen;

    public IndirectionNode(Generation gen) {
        this(null, gen);
    }

    public IndirectionNode(CompressedNode<K,V> cn, Generation gen) {
        this.gen = gen;
        setMainNode(cn);
    }

    public Generation getGen() {
        return gen;
    }

    void setMainNode(MainNode<K,V> newVal) {
        UPDATER.set(this, newVal);
    }

    boolean compareAndSetMainNode(MainNode<K,V> oldVal, MainNode<K,V> newVal) {
        return UPDATER.compareAndSet(this, oldVal, newVal);
    }

    public MainNode<K,V> readCommittedMainNode(ConcurrentTrie<K, V> trie) {
        if (mainNode == null || mainNode.prev == null) return mainNode;
        return finalizeCompareAndSetOperation(mainNode, trie);
    }

    private MainNode<K, V> finalizeCompareAndSetOperation(MainNode<K,V> m, ConcurrentTrie<K,V> ct) {
        while (m != null) {
            MainNode<K,V> prevVal = m.prev;
            IndirectionNode<K,V> ctr = ct.RDCSS_READ_ROOT(true);
            if (prevVal == null) return m;
            else if (prevVal instanceof FailedNode<?,?>) {
                // Try to commit to previous value.
                if (compareAndSetMainNode(m, ((FailedNode<K, V>) prevVal).prev)) {
                    return ((FailedNode<K, V>) prevVal).prev;
                }
            } else {
                if (ctr.gen.equals(gen) && ct.nonReadOnly()) {
                    if (m.CAS_PREV(prevVal, null)) return m;
                    else finalizeCompareAndSetOperation(m, ct);
                } else {
                    m.CAS_PREV(prevVal, new FailedNode<>(prevVal));
                    finalizeCompareAndSetOperation(m, ct);
                }
            }
            m = mainNode;
        }
        return null;
    }

    private boolean compareAndSetWithFinalize(MainNode<K,V> oldVal, MainNode<K,V> newVal, ConcurrentTrie<K,V> trie) {
        // Set oldVal as newVal's prev.
        newVal.WRITE_PREV(oldVal);
        if (compareAndSetMainNode(oldVal, newVal)) {
            finalizeCompareAndSetOperation(newVal, trie);
            return newVal.prev == null;
        }
        return false;
    }

    private IndirectionNode<K, V> inode(MainNode<K,V> cn) {
        IndirectionNode<K,V> nin = new IndirectionNode<>(gen);
        nin.setMainNode(cn);
        return nin;
    }

    public IndirectionNode<K, V> copyToGen(Generation newGen, ConcurrentTrie<K, V> trie) {
        IndirectionNode<K,V> nin = new IndirectionNode<>(newGen);
        MainNode<K,V> main = readCommittedMainNode(trie);
        nin.setMainNode(main);
        return nin;
    }

    public boolean recInsert(K k, V v, Integer hc, Integer lev, IndirectionNode<K,V> parent, Generation startGen, ConcurrentTrie<K,V> trie) {
        MainNode<K,V> m = readCommittedMainNode(trie);
        if (m instanceof CompressedNode) {
            CompressedNode<K,V> cn = (CompressedNode<K,V>) m;
            int idx = (hc >>> lev) & 0x1f;
            int flag = 1 << idx;
            int bmp = cn.bitmap;
            int mask = flag - 1;
            int pos = Integer.bitCount(bmp & mask);
            if ((bmp & flag) != 0) {
                if (cn.array[pos] instanceof IndirectionNode) {
                    @SuppressWarnings("unchecked")
                    IndirectionNode<K, V> in = (IndirectionNode<K, V>) cn.array[pos];
                    if (startGen == in.gen) {
                        return in.recInsert(k, v, hc, lev + 5, this, startGen, trie);
                    } else {
                        if (compareAndSetWithFinalize(cn, cn.renewed(startGen, trie), trie)) {
                            // Maybe put function in while (true) loop to avoid tail recursion and accidental
                            // stack overflow.
                            return recInsert(k, v, hc, lev, parent, startGen, trie);
                        } else {
                            return false;
                        }
                    }
                } else if (cn.array[pos] instanceof SingletonNode<?,?>) {
                    @SuppressWarnings("unchecked")
                    SingletonNode<K,V> sn = (SingletonNode<K, V>) cn.array[pos];
                    if (sn.getHash() == hc && sn.getKey().equals(k)) {
                        return compareAndSetWithFinalize(cn, cn.updatedAt(pos, new SingletonNode<>(k, v, hc), gen), trie);
                    } else {
                        CompressedNode<K,V> rn = cn.generation == gen ? cn : cn.renewed(gen, trie);
                        CompressedNode<K,V> nn = rn.updatedAt(pos, inode(CNodeUtil.createDualNode(sn, sn.getHash(), new SingletonNode<>(k, v, hc), hc, lev + 5, gen)), gen);
                        return compareAndSetWithFinalize(cn, nn, trie);
                    }
                }
            } else {
                CompressedNode<K,V> rn = cn.generation == gen ? cn : cn.renewed(gen, trie);
                CompressedNode<K,V> ncnode = rn.insertedAt(pos, flag, new SingletonNode<>(k, v, hc), gen);
                return compareAndSetWithFinalize(cn, ncnode, trie);
            }
        } else if (m instanceof TombNode<K,V>) {
            clean(parent, trie, lev - 5);
            return false;
        } else if (m instanceof ListNode) {
            ListNode<K,V> ln = (ListNode<K, V>) m;
            MainNode<K,V> nn = ln.inserted(k, v);
            return compareAndSetWithFinalize(ln, nn, trie);
        }
        return false;
    }

    public final Optional<V> recInsertIf(K k, V v, int hc, Object cond, int lev, IndirectionNode<K,V> parent, Generation startGen, ConcurrentTrie<K,V> trie) {
        MainNode<K,V> m = readCommittedMainNode(trie);
        if (m instanceof CompressedNode) {
            CompressedNode<K,V> cn = (CompressedNode<K, V>) m;
            int idx = (hc >>> lev) & 0x1f;
            int flag = 1 << idx;
            int bmp = cn.bitmap;
            int mask = flag - 1;
            int pos = Integer.bitCount(bmp & mask);
            if ((bmp & flag) != 0) {
                if (cn.array[pos] instanceof IndirectionNode) {
                    @SuppressWarnings("unchecked")
                    IndirectionNode<K,V> in = (IndirectionNode<K,V>) cn.array[pos];
                    if (startGen == in.gen) {
                        return in.recInsertIf(k, v, hc, cond, lev + 5, this, startGen, trie);
                    } else {
                        if (compareAndSetWithFinalize(cn, cn.renewed(startGen, trie), trie)) {
                            return recInsertIf(k, v, hc, cond, lev, parent, startGen, trie);
                        } else {
                            return Optional.empty();
                        }
                    }
                } else if (cn.array[pos] instanceof SingletonNode<?,?>) {
                    @SuppressWarnings("unchecked")
                    SingletonNode<K,V> sn = (SingletonNode<K, V>) cn.array[pos];
                    if (cond == null) {
                        if (sn.getHash() == hc && sn.getKey().equals(k)) {
                            if (compareAndSetWithFinalize(cn, cn.updatedAt(pos, new SingletonNode<>(k, v, hc), gen), trie)) {
                                return Optional.of(sn.getValue());
                            } else {
                                return null;
                            }
                        } else {
                            CompressedNode<K, V> rn = cn.generation == gen ? cn : cn.renewed(gen, trie);
                            MainNode<K, V> nn = rn.updatedAt(pos, inode(CNodeUtil.createDualNode(sn, sn.getHash(), new SingletonNode<>(k, v, hc), hc, lev + 5, gen)), gen);
                            if (compareAndSetWithFinalize(cn, nn, trie)) {
                                return Optional.empty();
                            } else {
                                return null;
                            }
                        }
                    } else if (cond == INodeUtil.KEY_ABSENT) {
                        if (sn.getHash() == hc && sn.getKey().equals(k)) {
                            return Optional.of(sn.getValue());
                        } else {
                            CompressedNode<K,V> rn = cn.generation == gen ? cn : cn.renewed(gen, trie);
                            MainNode<K,V> nn = rn.updatedAt(pos, inode(CNodeUtil.createDualNode(sn, sn.getHash(), new SingletonNode<>(k, v, hc), hc, lev + 5, gen)), gen);
                            if (compareAndSetWithFinalize(cn, nn, trie)) {
                                return Optional.empty();
                            } else {
                                return null;
                            }
                        }
                    } else if (cond == INodeUtil.KEY_PRESENT) {
                        if (sn.getHash() == hc && sn.getKey().equals(k)) {
                            if (compareAndSetWithFinalize(cn, cn.updatedAt(pos, new SingletonNode<>(k, v, hc), gen), trie)) {
                                return Optional.of(sn.getValue());
                            } else {
                                return null;
                            }
                        } else {
                            return Optional.empty();
                        }
                    } else {
                        @SuppressWarnings("unchecked")
                        V otherv = (V) cond;
                        if (sn.getHash() == hc && sn.getKey().equals(k) && sn.getValue().equals(otherv)) {
                            if (compareAndSetWithFinalize(cn, cn.updatedAt(pos, new SingletonNode<>(k, v, hc), gen), trie)) {
                                return Optional.of(sn.getValue());
                            } else {
                                return null;
                            }
                        } else {
                            return Optional.empty();
                        }
                    }
                }
            } else if (cond == null || cond == INodeUtil.KEY_ABSENT) {
                CompressedNode<K, V> rn = cn.generation == gen ? cn : cn.renewed(gen, trie);
                CompressedNode<K, V> ncnode = rn.insertedAt(pos, flag, new SingletonNode<>(k, v, hc), gen);
                if (compareAndSetWithFinalize(cn, ncnode, trie)) {
                    return Optional.empty();
                } else {
                    return null;
                }
            } else if (cond == INodeUtil.KEY_PRESENT) {
                return Optional.empty();
            } else {
                return Optional.empty();
            }
        } else if (m instanceof TombNode<K,V>) {
            clean(parent, trie, lev - 5);
            return null;
        } else if (m instanceof ListNode<K,V>) {
            ListNode<K,V> ln = (ListNode<K,V>) m;
            V vValue = ln.get(k);
            if (cond == null) {
                if (insertln(ln, k, v, trie))
                    return Optional.of(vValue);
                return null;
            } else if (cond == INodeUtil.KEY_ABSENT) {
                if (vValue == null) {
                    if (insertln(ln, k, v, trie))
                        return Optional.empty();
                    return null;
                }
                return Optional.of(vValue);
            } else if (cond == INodeUtil.KEY_PRESENT) {
                if (vValue != null) {
                    if (insertln(ln, k, v, trie))
                        return Optional.of(vValue);
                    return null;
                }
                return null;
            } else {
                if (vValue != null) {
                    if (vValue == cond) {
                        if (insertln(ln, k, v, trie))
                            return Optional.of(vValue);
                        return null;
                    }
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private boolean insertln(ListNode<K, V> listNode, K k, V v,  ConcurrentTrie<K, V> trie) {
        ListNode<K, V> nn = listNode.inserted(k, v);
        return compareAndSetWithFinalize(listNode, nn, trie);
    }

    public final Object recLookup(K k, int hc, int lev, IndirectionNode<K,V> parent, Generation startGen, ConcurrentTrie<K,V> trie) {
        MainNode<K,V> m = readCommittedMainNode(trie);
        if (m instanceof CompressedNode) {
            CompressedNode<K,V> cn = (CompressedNode<K,V>) m;
            int idx = (hc >>> lev) & 0x1f;
            int flag = 1 << idx;
            int bmp = cn.bitmap;
            if ((bmp & flag) == 0) {
                return null;
            } else {
                int pos = (bmp == 0xffffffff) ? idx : Integer.bitCount (bmp & (flag - 1));
                BasicNode sub = cn.array[pos];
                if (sub instanceof IndirectionNode) {
                    IndirectionNode<K,V> in = (IndirectionNode<K,V>) sub;
                    if (trie.isReadOnly() || (startGen == in.gen)) {
                        return in.recLookup(k, hc, lev + 5, this, startGen, trie);
                    } else {
                        if (compareAndSetWithFinalize(cn, cn.renewed(startGen, trie), trie)) {
                            return recLookup(k, hc, lev, parent, startGen, trie);
                        } else {
                            return RESTART;
                        }
                    }
                } else if (sub instanceof SingletonNode<?,?>) {
                    SingletonNode<K,V> sn = (SingletonNode<K, V>) sub;
                    if (sn.getHash() == hc && sn.getKey().equals(k)) {
                        return sn.getValue();
                    } else {
                        return null;
                    }
                }
            }
        } else if (m instanceof TombNode<K,V>) {
            if (trie.nonReadOnly()) {
                clean(parent, trie, lev - 5);
                return RESTART;
            } else {
                TombNode<K,V> tn = (TombNode<K,V>) m;
                if (tn.getHash() == hc && tn.getKey().equals(k)) {
                    return tn.getValue();
                } else {
                    return null;
                }
            }
        } else if (m instanceof ListNode<K,V>) {
            return ((ListNode<K, V>) m).get(k);
        }
        throw new RuntimeException("Out of all cases defined.");
    }

    public final Optional<V> recRemove(K k, V v, int hc, int lev, IndirectionNode<K, V> parent, Generation startgen, ConcurrentTrie<K, V> trie) {
        MainNode<K, V> m = readCommittedMainNode(trie); // use -Yinline!

        if (m instanceof CompressedNode) {
            CompressedNode<K, V> cn = (CompressedNode<K, V>) m;
            int idx = (hc >>> lev) & 0x1f;
            int bmp = cn.bitmap;
            int flag = 1 << idx;
            if ((bmp & flag) == 0) {
                return Optional.empty();
            } else {
                int pos = Integer.bitCount(bmp & (flag - 1));
                BasicNode sub = cn.array[pos];
                Optional<V> res = null;
                if (sub instanceof IndirectionNode) {
                    IndirectionNode<K, V> in = (IndirectionNode<K, V>) sub;
                    if (startgen == in.gen) {
                        res = in.recRemove(k, v, hc, lev + 5, this, startgen, trie);
                    } else {
                        if (compareAndSetWithFinalize(cn, cn.renewed(startgen, trie), trie))
                            res = recRemove(k, v, hc, lev, parent, startgen, trie);
                    }
                } else if (sub instanceof SingletonNode<?,?>) {
                    SingletonNode<K, V> sn = (SingletonNode<K, V>) sub;
                    if (sn.getHash() == hc && sn.getKey().equals(k) && (v == null || sn.getValue().equals(v))) {
                        @SuppressWarnings("unchecked")
                        MainNode<K, V> ncn = (MainNode<K, V>) cn.removedAt(pos, flag, gen).toContracted(lev);
                        if (compareAndSetWithFinalize(cn, ncn, trie)) {
                            res = Optional.of(sn.getValue());
                        }
                    } else {
                        return Optional.empty();
                    }
                }

                if (res == null || !res.isPresent()) {
                    return res;
                } else {
                    if (parent != null) {
                        MainNode<K, V> n = readCommittedMainNode(trie);
                        if (n instanceof TombNode<?,?>)
                            cleanParent(this, res, parent, hc, lev, startgen, trie);
                    }
                    return res;
                }
            }
        } else if (m instanceof TombNode<K,V>) {
            clean(parent, trie, lev - 5);
            return Optional.empty();
        } else if (m instanceof ListNode) {
            ListNode<K, V> ln = (ListNode<K, V>) m;
            if (v == null) {
                @SuppressWarnings("unchecked")
                Optional<V> optv = (Optional<V>) Optional.ofNullable(ln.get(k));
                ListNode<K, V> nn = (ListNode<K, V>) ln.removed(k);
                if (compareAndSetWithFinalize(ln, nn, trie)) {
                    return optv;
                } else {
                    return Optional.empty();
                }
            } else {
                @SuppressWarnings("unchecked")
                Optional<V> optv = (Optional<V>) Optional.ofNullable(ln.get(k));
                if (optv.isPresent() && optv.get().equals(v)) {
                    ListNode<K, V> nn = (ListNode<K, V>) ln.removed(k);
                    if (compareAndSetWithFinalize(ln, nn, trie)) {
                        return optv;
                    } else {
                        return Optional.empty();
                    }
                } else {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private void cleanParent(IndirectionNode<K, V> self, Optional<V> res, IndirectionNode<K, V> parent, int hc, int lev, Generation startgen, ConcurrentTrie<K, V> ct) {
        if (res.isPresent() && parent != null) { // never tomb at root
            MainNode<K, V> n = readCommittedMainNode(ct);
            if (n instanceof TombNode<K,V>) {
                TombNode<K, V> tn = (TombNode<K, V>) n;
                cleanParentRecursive(parent, hc, lev - 5, tn, startgen, ct);
            }
        }
    }

    private void cleanParentRecursive(IndirectionNode<K, V> parent, int hc, int lev, Object nonlive, Generation startgen, ConcurrentTrie<K, V> ct) {
        MainNode<K, V> pm = parent.readCommittedMainNode(ct);
        if (pm instanceof CompressedNode) {
            CompressedNode<K, V> cn = (CompressedNode<K, V>) pm;
            int idx = (hc >>> lev) & 0x1f;
            int bmp = cn.bitmap;
            int flag = 1 << idx;
            if ((bmp & flag) == 0)
                // Nothing to remove. Return.
                return;
            // Work on cleaning up.
            int pos = Integer.bitCount(bmp & (flag - 1));
            BasicNode sub = cn.array[pos];
            if (sub == this) {
                if (nonlive instanceof TombNode<?,?>) {
                    TombNode<K, V> tombNode = (TombNode<K, V>) nonlive;
                    MainNode<K, V> ncn = (MainNode<K, V>) cn.updatedAt(pos, tombNode.copyUntombed(), gen).toContracted(lev - 5);
                    if (!parent.compareAndSetWithFinalize(cn, ncn, ct)) {
                        if (ct.RDCSS_READ_ROOT(false).gen == startgen)
                            cleanParentRecursive(parent, hc, lev, nonlive, startgen, ct);
                    }
                }
            }
        } else {
            // Do nothing.
        }
    }

    private void clean(IndirectionNode<K,V> nd, ConcurrentTrie<K,V> ct, int lev) {
        MainNode<K,V> m = nd.readCommittedMainNode(ct);
        if (m instanceof CompressedNode<?,?>) {
            nd.compareAndSetWithFinalize(m, (MainNode<K, V>) ((CompressedNode<K,V>) m).toCompressed(ct, lev, gen), ct);
        }
    }

    final boolean isNullInode(ConcurrentTrie<K,V> ct) {
        return readCommittedMainNode(ct) == null;
    }

    public String string(int lev) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ".repeat(lev));
        sb.append("INode -> ");
        MainNode<K,V> mainnode = this.mainNode;
        if (mainnode == null) {
            sb.append("<null>");
        } else if (mainnode instanceof TombNode<K,V>) {
            TombNode<K,V> tn = (TombNode<K, V>) mainnode;
            sb.append("TNode(").append(tn.getKey()).append(", ").append(tn.getValue()).append(", ").append(tn.getHash()).append(", !)");
        } else if (mainnode instanceof CompressedNode) {
            CompressedNode<K,V> cn = (CompressedNode<K,V>) mainnode;
            sb.append(cn.string(lev));
        } else if (mainnode instanceof ListNode) {
            ListNode<K,V> ln = (ListNode<K,V>) mainnode;
            sb.append(ln.toString(lev));
        } else {
            sb.append("<elem: ").append(mainnode).append(">");
        }
        return sb.toString();
    }

    static <K,V> IndirectionNode<K,V> newRootNode() {
        Generation gen = new Generation();
        CompressedNode<K,V> cn = new CompressedNode<>(0, new BasicNode[0], gen);
        return new IndirectionNode<>(cn, gen);
    }

    /**
     * Returns a string representation of the indirection node.
     *
     * @param level The level in the trie.
     * @return The string representation.
     */
    public String toString(int level) {
        String indent = "  ".repeat(level);
        String mainNodeString;
        if (mainNode == null) {
            mainNodeString = "<null>";
        } else if (mainNode instanceof TombNode) {
            TombNode<K, V> tn = (TombNode<K, V>) mainNode;
            mainNodeString = String.format("TombNode(%s, %s, %d, !)", tn.getKey(), tn.getValue(), tn.getHash());
        } else if (mainNode instanceof CompressedNode) {
            CompressedNode<K, V> cn = (CompressedNode<K, V>) mainNode;
            mainNodeString = cn.toString(level);
        } else if (mainNode instanceof ListNode) {
            ListNode<K, V> ln = (ListNode<K, V>) mainNode;
            mainNodeString = ln.toString(level);
        } else {
            mainNodeString = String.format("<elem: %s>", mainNode.toString());
        }

        return String.format("%sIndirectionNode -> %s", indent, mainNodeString);
    }
}
