package nachos.threads;

import nachos.machine.*;

/**
 * A multi-threaded OS kernel.
 */
public class ThreadedKernel extends Kernel {
    /**
     * Allocate a new multi-threaded kernel.
     */
    public ThreadedKernel() {
        super();
    }

    /**
     * Initialize this kernel. Creates a scheduler, the first thread, and an
     * alarm, and enables interrupts. Creates a file system if necessary.
     */
    public void initialize(String[] args) {
        System.out.println("to here ThreadedKernel");
        // set scheduler
        String schedulerName = Config.getString("ThreadedKernel.scheduler");
        scheduler = (Scheduler) Lib.constructObject(schedulerName);

        // set fileSystem
        String fileSystemName = Config.getString("ThreadedKernel.fileSystem");
        if (fileSystemName != null)
            fileSystem = (FileSystem) Lib.constructObject(fileSystemName);
        else if (Machine.stubFileSystem() != null)
            fileSystem = Machine.stubFileSystem();
        else
            fileSystem = null;

        // start threading
        new KThread(null);

        alarm = new Alarm();

        Machine.interrupt().enable();
    }

    /**
     * Test this kernel. Test the <tt>KThread</tt>, <tt>Semaphore</tt>,
     * <tt>SynchList</tt>, and <tt>ElevatorBank</tt> classes. Note that the
     * autograder never calls this method, so it is safe to put additional
     * tests here.
     */
    public void selfTest() {

        // 内核任务不可中断
//        KThread.idleTest();

        // 测试 join()、waitUntil()
        KThread.selfTest();
        // 这个函数没有我们自己的实现，所以不运行
//        Semaphore.selfTest();

        // 用SynchList测试 Condition2
        SynchList.selfTest();

        // 测试运行时优先级的更改
        KThread.priorityScheduleTest();
        // 测试优先级的继承（through lock）
        KThread.priorityInheritanceTest();
        // 测试优先级的继承（through join）
        KThread.priorityInheritanceTest2();
        // 测试生产者-消费者程序
        Communicator.showDemo();
        // 测试坐船游戏
        Boat.selfTest();

    }

    /**
     * A threaded kernel does not run user programs, so this method does
     * nothing.
     */
    public void run() {
        System.out.println("\nrun user programs\n");
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
        Machine.halt();
    }

    /**
     * Globally accessible reference to the scheduler.
     */
    public static Scheduler scheduler = null;
    /**
     * Globally accessible reference to the alarm.
     */
    public static Alarm alarm = null;
    /**
     * Globally accessible reference to the file system.
     */
    public static FileSystem fileSystem = null;

    // dummy variables to make javac smarter
    private static RoundRobinScheduler dummy1 = null;
    private static PriorityScheduler dummy2 = null;
    private static LotteryScheduler dummy3 = null;
    private static Condition2 dummy4 = null;
    private static Communicator dummy5 = null;
    private static Rider dummy6 = null;
    private static ElevatorController dummy7 = null;
}
