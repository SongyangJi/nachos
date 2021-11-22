package nachos.threads;


import java.util.LinkedList;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {

    static final int N = 3;

    final Lock lock;
    final Condition hasProducer;
    final Condition hasConsumer;

    int numOfWaitingProducers, numOfWaitingConsumers;

    LinkedList<Integer> buffer = new LinkedList<>();

    boolean hasConsumersWaiting() {
        return numOfWaitingConsumers > 0;
    }

    boolean hasProducersWaiting() {
        return numOfWaitingProducers > 0;
    }


    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();
        hasProducer = new Condition1(lock);
        hasConsumer = new Condition1(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param word the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();

        System.out.println(KThread.currentThread().getName() + " says " + word);
        buffer.add(word);

        if (hasConsumersWaiting()) {
            --numOfWaitingConsumers;
            hasProducer.wake();
        } else {
            ++numOfWaitingProducers;
            hasConsumer.sleep();
        }

        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return the integer transferred.
     */
    public int listen() {
        int message;
        lock.acquire();

        if (hasProducersWaiting()) {
            --numOfWaitingProducers;
            hasConsumer.wake(); // 保证下面的 message 正是由 wake 唤醒的线程所 speak 的
        } else {
            ++numOfWaitingConsumers;
            hasProducer.sleep();
        }
        message = buffer.removeFirst();

        lock.release();
        return message;
    }

    public static void delay() {
        KThread.yield();
    }

    private static class Speaker implements Runnable {
        Communicator communicator;
        int which;

        static int id = 0;

        public Speaker(Communicator communicator, int which) {
            this.communicator = communicator;
            this.which = which;
        }

        @Override
        public void run() {
            for (int i = 0; i < N; i++) {
                int message = (++id);
                communicator.speak(message);
                delay();
            }
        }
    }

    private static class Listener implements Runnable {
        Communicator communicator;
        int which;

        public Listener(Communicator communicator, int which) {
            this.communicator = communicator;
            this.which = which;
        }

        @Override
        public void run() {
            for (int i = 0; i < N; i++) {
                System.out.println("try to listen ... ");
                System.out.println(KThread.currentThread().getName() + " hears " + communicator.listen());
                delay();
            }
        }
    }

    public static void showDemo() {
        System.out.println("\nto Communicator demo ********************************************************************\n");

        Communicator communicator = new Communicator();
        for (int i = 0; i < 3; i++) {
            new KThread(new Listener(communicator, i)).setName("Listener-" + i).fork();
            new KThread(new Speaker(communicator, i)).setName("Speaker-" + i).fork();
        }

        KThread.yield();

    }


}
