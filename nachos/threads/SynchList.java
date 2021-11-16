package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * A synchronized queue.
 */
public class SynchList {
    /**
     * Allocate a new synchronized queue.
     */
    public SynchList() {
        list = new LinkedList<>();
        lock = new Lock();
        listEmpty = new Condition2(lock);
    }

    /**
     * Add the specified object to the end of the queue. If another thread is
     * waiting in <tt>removeFirst()</tt>, it is woken up.
     *
     * @param o the object to add. Must not be <tt>null</tt>.
     */
    public void add(Object o) {
        Lib.assertTrue(o != null);

        // 下面的写法与JUC的Lock、Condition的 用法如出一辙
        lock.acquire();
        list.add(o);
        listEmpty.wake();
        lock.release();
    }

    /**
     * Remove an object from the front of the queue, blocking until the queue
     * is non-empty if necessary.
     *
     * @return the element removed from the front of the queue.
     */
    public Object removeFirst() {
        Object o;

        lock.acquire();
        while (list.isEmpty()) // 注意 while
            listEmpty.sleep();
        o = list.removeFirst();
        lock.release();

        return o;
    }

    private static class PingTest implements Runnable {
        PingTest(SynchList ping, SynchList pong) {
            this.ping = ping;
            this.pong = pong;
        }

        public void run() {
            for (int i = 0; i < 10; i++)
                pong.add(ping.removeFirst());
        }

        private final SynchList ping;
        private final SynchList pong;
    }

    /**
     * Test that this module is working.
     */
    public static void selfTest() {
        System.out.println("\nto SynchList Test ********************************************************************\n");

        SynchList ping = new SynchList();
        SynchList pong = new SynchList();

        new KThread(new PingTest(ping, pong)).setName("ping").fork();

        for (int i = 0; i < 10; i++) {
            Integer o = i;
            ping.add(o);
            System.out.println("blockingQueue add i : " + i);
            Lib.assertTrue(pong.removeFirst() == o);
        }
    }

    private final LinkedList<Object> list;
    private final Lock lock;
    private final Condition listEmpty;
}

