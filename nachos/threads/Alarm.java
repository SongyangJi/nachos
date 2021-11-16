package nachos.threads;

import nachos.machine.*;

import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
        this.waitingThreadQueue = new PriorityQueue<>();
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() {
//                System.out.println("here cur thread "+Thread.currentThread().getName());
                handleWaitingThreads();
                timerInterrupt();
            }
        });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
//        KThread.currentThread().yield(); 我觉得原始代码是想表达如下意思

        Lib.assertTrue(KThread.currentThread() != null);
        Lib.assertTrue(Machine.interrupt().disabled(),"当前屏蔽中断");
        KThread.yield();
    }

    private void handleWaitingThreads() {
        while (!waitingThreadQueue.isEmpty()) {
            long currentTime = Machine.timer().getTime();
            if (waitingThreadQueue.peek().wakeTime > currentTime) break;

            boolean intStatus = Machine.interrupt().disable();

            WaitingThread waitingThread = waitingThreadQueue.poll();
            if (waitingThread != null) {
                waitingThread.thread.ready();
            }
            Machine.interrupt().restore(intStatus);
        }
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param x the minimum number of clock ticks to wait.
     * @see nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {

        long wakeTime = Machine.timer().getTime() + x;
        waitingThreadQueue.add(new WaitingThread(wakeTime, KThread.currentThread()));

        boolean intStatus = Machine.interrupt().disable();

        KThread.sleep();

        Machine.interrupt().restore(intStatus);


        // for now, cheat just to get something working (busy waiting is bad)
//        while (wakeTime > Machine.timer().getTime())
//            KThread.yield();
    }


    private final PriorityQueue<WaitingThread> waitingThreadQueue;


    private static class WaitingThread implements Comparable<WaitingThread> {
        long wakeTime;
        KThread thread;

        public WaitingThread(long wakeTime, KThread thread) {
            Lib.assertTrue(thread != null);
            this.wakeTime = wakeTime;
            this.thread = thread;
        }

        @Override
        public int compareTo(WaitingThread o) {
            return wakeTime - o.wakeTime < 0 ? -1 : 1;
        }
    }

}
