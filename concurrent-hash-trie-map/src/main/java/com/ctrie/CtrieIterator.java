package com.ctrie;

import com.ctrie.node.BasicNode;
import com.ctrie.node.CompressedNode;
import com.ctrie.node.IndirectionNode;
import com.ctrie.node.KeyValueNode;
import com.ctrie.node.ListNode;
import com.ctrie.node.MainNode;
import com.ctrie.node.SingletonNode;
import com.ctrie.node.TombNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class CtrieIterator<K, V> implements Iterator<Map.Entry<K, V>> {
    private final ConcurrentTrie<K, V> ct;
    private final BasicNode[][] stack = new BasicNode[7][];
    private final int[] stackpos = new int[7];
    private int depth = -1;
    private Iterator<Map.Entry<K, V>> subiter = null;
    private KeyValueNode<K, V> current = null;

    public CtrieIterator(ConcurrentTrie<K, V> ct) {
        this(ct, true);
    }

    public CtrieIterator(ConcurrentTrie<K, V> ct, boolean mustInit) {
        this.ct = ct;
        if (mustInit) initialize();
    }

    @Override
    public boolean hasNext() {
        return current != null || subiter != null;
    }

    @Override
    public Map.Entry<K, V> next() {
        if (hasNext()) {
            Map.Entry<K, V> r;
            if (subiter != null) {
                r = subiter.next();
                checkSubiter();
            } else {
                r = current.getKeyValuePair();
                advance();
            }
            return r;
        } else {
            throw new NoSuchElementException();
        }
    }

    private void readin(IndirectionNode<K, V> in) {
        MainNode<K, V> m = in.readCommittedMainNode(ct);
        if (m instanceof CompressedNode) {
            depth += 1;
            stack[depth] = ((CompressedNode<K, V>) m).array;
            stackpos[depth] = -1;
            advance();
        } else if (m instanceof TombNode<K, V>) {
            current = (TombNode<K, V>) m;
        } else if (m instanceof ListNode) {
            subiter = ((ListNode<K, V>) m).listMap.entrySet().iterator();
            checkSubiter();
        } else {
            current = null;
        }
    }

    private void checkSubiter() {
        if (!subiter.hasNext()) {
            subiter = null;
            advance();
        }
    }

    private void initialize() {
        assert ct.isReadOnly();
        IndirectionNode<K, V> r = ct.RDCSS_READ_ROOT(false);
        readin(r);
    }

    private void advance() {
        if (depth >= 0) {
            int npos = stackpos[depth] + 1;
            if (npos < stack[depth].length) {
                stackpos[depth] = npos;
                BasicNode sub = stack[depth][npos];
                if (sub instanceof SingletonNode<?, ?>) {
                    current = (SingletonNode<K, V>) sub;
                } else if (sub instanceof IndirectionNode) {
                    readin((IndirectionNode<K, V>) sub);
                }
            } else {
                depth -= 1;
                advance();
            }
        } else {
            current = null;
        }
    }

    protected List<Iterator<Map.Entry<K, V>>> subdivide() {
        if (subiter != null) {
            // the case where an LNode is being iterated
            Iterator<Map.Entry<K, V>> it = subiter;
            subiter = null;
            advance();
            return Arrays.asList(it, this);
        } else if (depth == -1) {
            return Collections.singletonList(this);
        } else {
            for (int d = 0; d <= depth; d++) {
                int rem = stack[d].length - 1 - stackpos[d];
                if (rem > 0) {
                    BasicNode[] arr1 = Arrays.copyOfRange(stack[d], stackpos[d] + 1, stackpos[d] + 1 + rem / 2);
                    BasicNode[] arr2 = Arrays.copyOfRange(stack[d], stackpos[d] + 1 + rem / 2, stack[d].length);
                    stack[d] = arr1;
                    stackpos[d] = -1;
                    CtrieIterator<K, V> it = new CtrieIterator<>(ct, false);
                    it.stack[0] = arr2;
                    it.stackpos[0] = -1;
                    it.depth = 0;
                    it.advance(); // <-- fix it
                    return Arrays.asList(this, it);
                }
            }
            return Collections.singletonList(this);
        }
    }

    private void print() {
        System.out.println("ctrie iterator");
        System.out.println(Arrays.toString(stackpos));
        System.out.println("depth: " + depth);
        System.out.println("current: " + current);
        System.out.println(Arrays.stream(stack).map(Arrays::toString).collect(Collectors.joining("\n")));
    }
}
