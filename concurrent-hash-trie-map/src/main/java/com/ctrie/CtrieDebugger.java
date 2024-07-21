package com.ctrie;

import java.util.concurrent.ConcurrentLinkedQueue;

class CtrieDebugger {
    private static final ConcurrentLinkedQueue<Object> logbuffer = new ConcurrentLinkedQueue<>();

    public static void log(Object s) {
        logbuffer.add(s);
    }

    public static void flush() {
        for (Object s : logbuffer) {
            System.out.println(s.toString());
        }
        logbuffer.clear();
    }

    public static void clear() {
        logbuffer.clear();
    }
}
