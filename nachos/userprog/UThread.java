package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.util.Arrays;

/**
 * A UThread is KThread that can execute user program code inside a user
 * process, in addition to Nachos kernel code.
 */
public class UThread extends KThread {
    /**
     * Allocate a new UThread.
     */
    public UThread(UserProcess process) {
        super();

        setTarget(new Runnable() {
            public void run() {
                runProgram();
            }
        });

        this.process = process;
    }

    public UThread(UserProcess process, int[] userRegisters) {
        super();

        setTarget(new Runnable() {
            public void run() {
                continueProgram();
            }
        });

        this.process = process;
        this.userRegisters = userRegisters;
    }


    private void continueProgram() {
//        for (int i = 0; i < 5000000; i++) {
//            KThread.yield();
//        }

        // TODO 初始化为父进程 fork 时的寄存器组
        for (int i = 0; i < Processor.numUserRegisters; i++) {
            Machine.processor().writeRegister(i, userRegisters[i]);
        }
        process.restoreState();

        // todo PC 前进
        Machine.processor().advancePC();

//         System.out.println("#3 当前寄存器组为 ： " + Arrays.toString(Processor.currentRegisters()));

        Machine.processor().run();

        Lib.assertNotReached();
    }


    private void runProgram() {
        // 初始化寄存器的值
        process.initRegisters();
        // 初始化调用
        process.restoreState();

        Machine.processor().run();

        Lib.assertNotReached();
    }

    /**
     * Save state before giving up the processor to another thread.
     * 将用户寄存区状态存储下来
     */
    protected void saveState() {
        process.saveState();

        for (int i = 0; i < Processor.numUserRegisters; i++)
            userRegisters[i] = Machine.processor().readRegister(i);

        super.saveState();
    }

    /**
     * Restore state before receiving the processor again.
     */
    protected void restoreState() {
        super.restoreState();

        for (int i = 0; i < Processor.numUserRegisters; i++) {
            Machine.processor().writeRegister(i, userRegisters[i]);
        }

        process.restoreState();
    }

    /**
     * Storage for the user register set.
     *
     * <p>
     * A thread capable of running user code actually has <i>two</i> sets of
     * CPU registers: one for its state while executing user code, and one for
     * its state while executing kernel code. While this thread is not running,
     * its user state is stored here.
     */
    public int[] userRegisters = new int[Processor.numUserRegisters];

    /**
     * The process to which this thread belongs.
     */
    public UserProcess process;
}
