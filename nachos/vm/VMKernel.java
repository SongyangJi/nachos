package nachos.vm;

import nachos.machine.Config;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.KThread;
import nachos.userprog.*;


/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
        super();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
        super.initialize(args);
        String opt = Config.getString("Page.replacement");
        if (opt == null || opt.equals("fifo")) {
            usedFrameManager = fifoUsedFrameManager;
        } else if (opt.equals("lru")) {
            System.out.println("to lru init");
            usedFrameManager = lruUsedFrameManager;
        } else if (opt.equals("sec")) {
            usedFrameManager = enhancedSecondChance;
        }
    }

    /**
     * Test this kernel.
     */
    public void selfTest() {
        super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {

        System.out.println("\nrun user programs\n");

        VMProcess process = new VMProcess();


        String shellProgram = Machine.getShellProgramName();
        String[] args = new String[]{};
        Lib.assertTrue(process.execute(shellProgram, args));

        Lib.assertTrue(KThread.currentThread() != null);

        KThread.finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
        super.terminate();
    }

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';


    public static UsedFrameManager usedFrameManager;

    public static final FifoUsedFrameManager fifoUsedFrameManager = new FifoUsedFrameManager();

    public static final LruUsedFrameManager lruUsedFrameManager = new LruUsedFrameManager();

    public static final EnhancedSecondChance enhancedSecondChance = new EnhancedSecondChance();

}
