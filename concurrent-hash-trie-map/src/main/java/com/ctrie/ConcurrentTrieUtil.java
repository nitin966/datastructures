package com.ctrie;

public class ConcurrentTrieUtil {

    public static <K> int computeHash(K k) {
        return k.hashCode();
    }
}
