package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;
import java.util.List;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 * <p>
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return the current thread.
     */
    public static KThread currentThread() {
        Lib.assertTrue(currentThread != null);
        return currentThread;
    }

    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
        if (currentThread != null) {
            tcb = new TCB();
        } else {
            readyQueue = ThreadedKernel.scheduler.newThreadQueue(false).setName("readyQueue");
            readyQueue.acquire(this);

            currentThread = this;
            tcb = TCB.currentTCB(); // 也正就是 Machine类在main方法里new的TCB
//            System.out.println("tcb : "+tcb);
            name = "KThread main";
            System.out.println("current KThread is " + name);
            System.out.println("current java Thread is " + Thread.currentThread().getName());

            restoreState();

            // 创建 do nothing 的 idle 线程，这个懒惰线程存在的目的是保证系统时钟的前进
            // 当所有的内核线程都陷入等待时（也就是就绪队列上没有内核线程时），
            // nacho 便会调度这个懒惰线程。
            createIdleThread();
        }
    }

    /**
     * Allocate a new KThread.
     *
     * @param target the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
        this();
        this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param target the object whose <tt>run</tt> method is called.
     * @return this thread.
     */
    public KThread setTarget(Runnable target) {
        Lib.assertTrue(status == statusNew);

        this.target = target;
        return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param name the name to give to this thread.
     * @return this thread.
     */
    public KThread setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return the name given to this thread.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return the full name given to this thread.
     */
    public String toString() {
        return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    public int compareTo(Object o) {
        KThread thread = (KThread) o;

        if (id < thread.id)
            return -1;
        else if (id > thread.id)
            return 1;
        else
            return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {
        Lib.assertTrue(status == statusNew);
        Lib.assertTrue(target != null);

        Lib.debug(dbgThread,
                "Forking thread: " + toString() + " Runnable: " + target);

        // todo 关中断
        boolean intStatus = Machine.interrupt().disable();

        tcb.start(new Runnable() {
            public void run() {
                // runThread() 方法仅在此处调用
                runThread();
            }
        });
        // 将此线程移动到就绪状态并将其添加到调度程序的就绪队列中
        ready();

        // todo 开中断
        Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
        begin();
        target.run();
        System.out.println("to finish...");
        finish();
    }

    private void begin() {
        Lib.debug(dbgThread, "Beginning thread: " + toString());

        Lib.assertTrue(this == currentThread);

        restoreState();

        // todo 开中断 ?（目前在该类中仅看到这一次开中断）
        Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     * <p>
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */
    public static void finish() {
        Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());

        Machine.interrupt().disable();

        Machine.autoGrader().finishingCurrentThread();

        Lib.assertTrue(toBeDestroyed == null);
        toBeDestroyed = currentThread;


        currentThread.status = statusFinished;

        // 将等待此内核线程终结的线程唤醒，加入ready队列
        ThreadQueue waitQueue = currentThread.waitMeFinishThreadsQueue;
        if (waitQueue != null) {
            KThread nextWakeThread;
            while ((nextWakeThread = waitQueue.nextThread()) != null) {
                nextWakeThread.ready();
            }
        }

        sleep(); // 目前不能立即删除此线程
    }

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */
    public static void yield() {
        Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());

        Lib.assertTrue(currentThread.status == statusRunning);

        // todo 为什么要先关中断？
        boolean intStatus = Machine.interrupt().disable();

        currentThread.ready();

        runNextThread();

        // 恢复之前的中断状态
        Machine.interrupt().restore(intStatus);
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
        Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());

        // todo 当前必屏蔽了中断
        Lib.assertTrue(Machine.interrupt().disabled());

        if (currentThread.status != statusFinished)
            currentThread.status = statusBlocked;

        // 放弃 CPU，因为当前线程要么已经完成，要么被阻塞。
        runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    public void ready() {
        Lib.debug(dbgThread, "Ready thread: " + toString());
        // todo 当前必屏蔽了中断
        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(status != statusReady);

        status = statusReady;
        if (this != idleThread) {
            // 将此线程加入 ready 队列，等待调度
            readyQueue.waitForAccess(this);
        }

        Machine.autoGrader().readyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
    public void join() {
        Lib.debug(dbgThread, "Joining to thread: " + toString());

        Lib.assertTrue(this != currentThread);
        // 开关中断的处理逻辑类似于 yield
        boolean intStatus = Machine.interrupt().disable();

        // 延迟初始化
        if (waitMeFinishThreadsQueue == null) {
            this.waitMeFinishThreadsQueue = ThreadedKernel.scheduler.newThreadQueue(true);
            waitMeFinishThreadsQueue.acquire(this); // todo
        }

        waitMeFinishThreadsQueue.waitForAccess(currentThread);
        sleep();

        Machine.interrupt().restore(intStatus);

    }

    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
        Lib.assertTrue(idleThread == null);

        idleThread = new KThread(new Runnable() {
            public void run() {
                while (true) KThread.yield();
            }
        });

        System.out.println("idleThread is " + idleThread.name);

        idleThread.setName("idle");

        Machine.autoGrader().setIdleThread(idleThread);

        idleThread.fork();
    }

    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
        KThread nextThread = readyQueue.nextThread();
        if (nextThread == null)
            nextThread = idleThread;

        // run() 仅在此处调用
        nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     */
    private void run() {
        // todo 当前必屏蔽了中断
        Lib.assertTrue(Machine.interrupt().disabled());

        Machine.yield();

        currentThread.saveState();

        Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
                + " to: " + toString());

        currentThread = this; // 当前 thread 成为 currentThread

        // tcb 上下文切换，对应的内核线程得到调度
        tcb.contextSwitch();

        currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {
        Lib.debug(dbgThread, "Running thread: " + currentThread.toString());
        // todo 当前必屏蔽了中断
        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(this == currentThread);
        Lib.assertTrue(tcb == TCB.currentTCB());

        Machine.autoGrader().runningThread(this);

        status = statusRunning;

        // destroy toBeDestroyed
        if (toBeDestroyed != null) {
            toBeDestroyed.tcb.destroy();
            toBeDestroyed.tcb = null;
            toBeDestroyed = null;
        }
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
        // todo 当前必屏蔽了中断
        Lib.assertTrue(Machine.interrupt().disabled());
        Lib.assertTrue(this == currentThread);
    }

    private static class PingTest implements Runnable {
        PingTest(int which) {
            this.which = which;
        }

        public void run() {
            System.out.println("current thread wait for a while first ...");
            ThreadedKernel.alarm.waitUntil(10 * 2000000);
            System.out.println("current thread waiting finished..");

            for (int i = 0; i < 5; i++) {
//                System.out.println("current : " + Thread.currentThread().getName());
                System.out.println("*** thread " + which + " looped "
                        + i + " times");
                KThread.yield();
            }
        }

        private final int which;
    }

    private static void setPriority(KThread thread, int priority) {
        boolean inStatus = Machine.interrupt().disable();
        ThreadedKernel.scheduler.setPriority(thread, priority);
        Machine.interrupt().restore(inStatus);
    }

    private static void doIdleWork() {
        System.out.println(KThread.currentThread.getName() + " 开始工作");
        for (int i = 0; i < 10; i++) {
            KThread.yield();
            System.out.println(KThread.currentThread.getName() + " 正在工作~" + i);
        }
        System.out.println(KThread.currentThread.getName() + " finished...");
    }


    public static void priorityInheritanceTest() {

        System.out.println("\nto priorityInheritanceTest ********************************************************************\n");

        Lock lock = new Lock();

        KThread kThread2 = new KThread(new Runnable() {
            @Override
            public void run() {
                System.out.println(KThread.currentThread.getName() + " 开始工作");
                lock.acquire();
                System.out.println("线程2加锁");


                KThread kThread3 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        doIdleWork();
                    }
                }).setName("优先级-3的线程");

                KThread kThread4 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        doIdleWork();
                    }
                }).setName("优先级-4的线程");


                KThread kThread7 = new KThread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("线程7尝试加锁");
                        lock.acquire();
                        System.out.println("线程7获取到锁");
                        lock.release();


                        doIdleWork();
                    }
                }).setName("优先级-7的线程");

                setPriority(kThread3, 3);
                setPriority(kThread4, 4);
                setPriority(kThread7, 7);

                kThread3.fork();
                kThread4.fork();
                kThread7.fork();

                for (int i = 0; i < 10; i++) {
                    if (i == 5) {
                        System.out.println(KThread.currentThread.getName() + " 释放锁");
                        lock.release();
                    }
                    KThread.yield();
                    System.out.println(KThread.currentThread.getName() + " 正在工作~" + i);

                    showCurrentThreadEffectivePriority();
                }
                System.out.println(KThread.currentThread.getName() + " finished...");
            }
        }).setName("优先级-2的线程");

        setPriority(kThread2, 2);
        kThread2.fork();


        KThread.yield();

    }

    private static void showCurrentThreadEffectivePriority() {
        boolean intStatus = Machine.interrupt().disable();
        System.out.println("它的有效优先级为： "+ThreadedKernel.scheduler.getEffectivePriority());
        Machine.interrupt().restore(intStatus);
    }

    public static void priorityInheritanceTest2() {

        System.out.println("\nto priorityInheritanceTest2 ********************************************************************\n");


        KThread kThread2 = new KThread(new Runnable() {
            @Override
            public void run() {
                System.out.println(KThread.currentThread.getName() + " 开始工作");

                for (int i = 0; i < 10; i++) {
                    KThread.yield();
                    System.out.println(KThread.currentThread.getName() + " 正在工作~" + i);
                    showCurrentThreadEffectivePriority();
                }
                System.out.println(KThread.currentThread.getName() + " finished...");
            }
        }).setName("优先级-2的线程");



        KThread kThread3 = new KThread(new Runnable() {
            @Override
            public void run() {
                doIdleWork();
            }
        }).setName("优先级-3的线程");

        KThread kThread4 = new KThread(new Runnable() {
            @Override
            public void run() {
                doIdleWork();
            }
        }).setName("优先级-4的线程");


        KThread kThread7 = new KThread(new Runnable() {
            @Override
            public void run() {
                kThread2.join();
                doIdleWork();
            }
        }).setName("优先级-7的线程");


        setPriority(kThread2, 2);
        setPriority(kThread3, 3);
        setPriority(kThread4, 4);
        setPriority(kThread7, 7);

        kThread2.fork();
        kThread3.fork();
        kThread4.fork();
        kThread7.fork();

        KThread.yield();
    }


    public static void priorityScheduleTest() {
        System.out.println("\nto PriorityScheduleTest ********************************************************************\n");

        KThread thread2 = new KThread(new Runnable() {
            @Override
            public void run() {
                doIdleWork();
            }
        }).setName("优先级-2的线程");


        KThread thread3 = new KThread(new Runnable() {
            @Override
            public void run() {
                doIdleWork();
            }
        }).setName("优先级-3的线程");

        KThread thread4 = new KThread(new Runnable() {
            @Override
            public void run() {
                System.out.println(KThread.currentThread.getName() + " 开始工作");
                for (int i = 0; i < 1000000; i++) {
                    if (i == 500000) {
                        System.out.print(KThread.currentThread.getName() + "修改自身的优先级为 " + 1);
                        setPriority(KThread.currentThread(), 1);
                        System.out.println("，并且修改 " + thread3.getName() + " 的优先级为" + 7);
                        setPriority(thread3, 7);
                    }
                    KThread.yield();
                }
                System.out.println(KThread.currentThread.getName() + " finished...");
            }
        }).setName("优先级-4的线程");

        setPriority(thread2, 2);
        setPriority(thread3, 3);
        setPriority(thread4, 4);

        thread2.fork();
        thread3.fork();
        thread4.fork();


        setPriority(KThread.currentThread(), 0);
        KThread.yield();

    }

    public static void idleTest() {
        new KThread(new Runnable() {
            @Override
            public void run() {
                while (true) ;
            }
        }).setName("test-idle").fork();
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
        Lib.debug(dbgThread, "Enter KThread.selfTest");

        System.out.println("\nto KThreadTest here ********************************************************************\n");

        KThread thread1 = new KThread(new PingTest(1)).setName("forked thread");
        thread1.fork();

        thread1.join();

        new PingTest(0).run();
    }


    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;

    /**
     * 进程的状态枚举量
     */
    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */
    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;

    /**
     * Unique identifier for this thread. Used to deterministically compare
     * threads.
     */
    private final int id = numCreated++;
    /**
     * Number of times the KThread constructor was called.
     */
    private static int numCreated = 0;

    private static ThreadQueue readyQueue = null;
    private static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;

    // join 的实现 from jsy
    ThreadQueue waitMeFinishThreadsQueue = null;

}
