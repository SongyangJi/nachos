package nachos.threads;

import nachos.machine.*;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }

    /**
     * Allocate a new priority thread queue.
     *
     * @param transferPriority <tt>true</tt> if this queue should
     *                         transfer priority from waiting threads
     *                         to the owning thread.
     * @return a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
    }

    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());

        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());

        Lib.assertTrue(priority >= priorityMinimum &&
                priority <= priorityMaximum);

        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority + 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();

        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority - 1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param thread the thread whose scheduling state to return.
     * @return the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);

        return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {

        protected ThreadState resourceHolder = null;


        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());

            ThreadState threadState = getThreadState(thread);
            threadState.acquire(this); // 先执行断言操作

//            System.out.println("执行断言: "+resourceHolder.thread.getName());
            Lib.assertTrue(resourceHolder == null);
            resourceHolder = threadState;
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me
            if (waitingSet.isEmpty()) {
                resourceHolder = null; // todo
                return null;
            }

            ThreadState threadState = waitingSet.pollFirst();
            KThread thread = null;
            if (threadState != null) {
                Lib.assertTrue(threadState.waitQueue != null);
                if (this.resourceHolder != null && threadState.waitQueue.transferPriority) {
                    this.resourceHolder.resetOriginalPriority(); // 有效优先级复位
                }
                threadState.waitQueue = null; // todo 出队时置空, 表明此线程不再等待某个资源
                thread = threadState.thread;
            }

            // 不论threadState为null还是不为null,更新 resourceHolder
            this.resourceHolder = threadState;
            return thread;
        }

        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return the next thread that <tt>nextThread()</tt> would
         * return.
         */
        protected ThreadState pickNextThread() {
            return waitingSet.first();
            // implement me
        }

        public void print() {
            // todo 当前必屏蔽了中断
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }


        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;

        protected final TreeSet<ThreadState> waitingSet = new TreeSet<>(new Comparator<ThreadState>() {
            @Override
            public int compare(ThreadState o1, ThreadState o2) {
                if (o1.id == o2.id) return 0;
                if (o1.getEffectivePriority() != o2.getEffectivePriority()) {
                    return o2.getEffectivePriority() - o1.getEffectivePriority();
                }
                if (o1.joinQueueTime != o2.joinQueueTime) {
                    return o1.joinQueueTime - o2.joinQueueTime < 0 ? -1 : 1;
                }
                return o1.id - o2.id < 0 ? -1 : 1;
            }
        });
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param thread the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
            this.priority = priorityDefault;
            this.effectivePriority = priorityDefault;
//            setPriority(priorityDefault);
            this.id = ++counter;
        }

        /**
         * Return the priority of the associated thread.
         *
         * @return the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Return the effective priority of the associated thread.
         *
         * @return the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            // implement me
            return effectivePriority;
        }

        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param priority the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;
            // implement me

            this.priority = priority;

            this.setEffectivePriority(priority);

        }

        // 继承优先级，并动态调整优先队列
        private void setEffectivePriority(int effectivePriority) {
            if (this.effectivePriority == effectivePriority) {
                return;
            }


            if (this.waitQueue != null) {
                TreeSet<ThreadState> treeSet = this.waitQueue.waitingSet;

                Lib.assertTrue(treeSet.contains(this));


                // 注意是先remove
                treeSet.remove(this);
                this.effectivePriority = effectivePriority;
                treeSet.add(this);

            } else {
                this.effectivePriority = effectivePriority;
            }
        }


        /**
         * 优先级复位
         */
        private void resetOriginalPriority() {
            this.setEffectivePriority(this.priority);
        }

        /**
         * 优先级继承
         */
        private void transferPriority(int priority) {
            Lib.assertTrue(priority > this.getEffectivePriority());
            setEffectivePriority(priority);
        }

        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitingSet</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param waitQueue the queue that the associated thread is
         *                  now waiting on.
         * @see nachos.threads.ThreadQueue#waitForAccess
         */
        public void waitForAccess(PriorityQueue waitQueue) {
            // implement me

            // 入队时间
            this.waitQueue = waitQueue; // 相关的等待队列
            this.joinQueueTime = Machine.timer().getTime();

            waitQueue.waitingSet.add(this);


            ThreadState currentResourceHolder = waitQueue.resourceHolder;

            // todo  Lib.assertTrue(waitQueue.resourceHolder != null); 此断言是没必要的

            int higherEffectivePriority = this.getEffectivePriority();
            // 优先级链式传递，直到某个线程不处于等待状态
            if (currentResourceHolder != null
                    && higherEffectivePriority > currentResourceHolder.getEffectivePriority()) {
                PriorityQueue departure = waitQueue, destination = currentResourceHolder.waitQueue;

                while (destination != null &&
                        higherEffectivePriority > departure.resourceHolder.getPriority() &&
                        departure.transferPriority) {
                    departure.resourceHolder.transferPriority(higherEffectivePriority);
                    departure = destination;
                    if (destination.resourceHolder != null) {
                        destination = destination.resourceHolder.waitQueue;
                    } else {
                        destination = null;
                    }
                }
            }

        }


        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitingSet</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitingSet</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitingSet</tt>.
         *
         * @see nachos.threads.ThreadQueue#acquire
         * @see nachos.threads.ThreadQueue#nextThread
         */
        public void acquire(PriorityQueue waitQueue) {
            // implement me
            // 断言 无'人'等待

            // todo this.waitingSet = waitingSet;
            Lib.assertTrue(waitQueue.waitingSet.isEmpty());
        }

        /**
         * The thread with which this object is associated.
         */
        protected KThread thread;
        /**
         * The priority of the associated thread.
         */
        protected int priority;

        /**
         * 有效优先级
         */
        protected int effectivePriority;

        /**
         * 入队时间，在优先级相同的时候根据时间比较
         */
        protected long joinQueueTime;

        /**
         * 此 threadState 目前所属的优先级调度队列
         * 仅当线程无法获得资源时，有 waitingSet != null；
         * 换言之有如下不变量，
         * waitingSet == null || waitingSet.resourceHolder != this
         */
        protected PriorityQueue waitQueue;

        /**
         * 唯一 id
         */
        protected long id;


        @Override
        public String toString() {
            return "ThreadState{" +
                    "thread=" + thread.getName() + " 有效优先级为：" + getEffectivePriority() +
                    '}';
        }
    }

    private static long counter = 0L;
}
