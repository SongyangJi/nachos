package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
        super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
        super.initialize(args);

        console = new SynchConsole(Machine.console());

        // todo 设置用户指令导致的异常对应的处理函数
        Machine.processor().setExceptionHandler(new Runnable() {
            public void run() {
                exceptionHandler();
            }
        });
    }

    /**
     * Test the console device.
     */
    public void selfTest() {
        // todo 先去掉
//        super.selfTest();

//        System.out.println("Testing the console device. Typed characters");
//        System.out.println("will be echoed until q is typed.");

        // todo
//        char c;
//        do {
//            c = (char) console.readByte(true);
//            console.writeByte(c);
//        }
//        while (c != 'q');

        System.out.println();
    }

    /**
     * Returns the current process.
     *
     * @return the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
        if (!(KThread.currentThread() instanceof UThread))
            return null;

        return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
        Lib.assertTrue(KThread.currentThread() instanceof UThread);

        UserProcess process = ((UThread) KThread.currentThread()).process;
        int cause = Machine.processor().readRegister(Processor.regCause);
        process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see nachos.machine.Machine#getShellProgramName
     */
    public void run() {
        super.run();

        UserProcess process = UserProcess.newUserProcess();


        String shellProgram = Machine.getShellProgramName();
        // 入口 : process.execute
//        System.err.println("shell "+shellProgram);
        String[] args = new String[]{};
//        if (shellProgram.equals("test_fileio.coff")) {
//            String name = "test_text.txt";
//            args = new String[]{shellProgram, name};
//        }
        Lib.assertTrue(process.execute(shellProgram, args));

//        KThread.currentThread().finish();
        Lib.assertTrue(KThread.currentThread() != null);

//        System.out.println("cur : "+Thread.currentThread().getName());
//        System.out.println(KThread.currentThread().getName());
        KThread.finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
        super.terminate();
    }

    /**
     * Globally accessible reference to the synchronized console.
     */
    public static SynchConsole console;

    // dummy variables to make javac smarter
    private static Coff dummy1 = null;


    // todo 同步访问
    // 空闲帧列表
    private static final TreeSet<Integer> freePhysicalPages;

    static {
        freePhysicalPages = new TreeSet<>();
        int numPhysPages = Machine.processor().getNumPhysPages();
        for (int i = 0; i < numPhysPages; i++) {
            freePhysicalPages.add(i);
        }
    }

    public static int numFreePhysicalPages() {
        return freePhysicalPages.size();
    }

    public static List<Integer> allocatePhysicalPages(int numPages) {
        if (freePhysicalPages.size() < numPages) {
            return null;
        }

        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < numPages; i++) {
            list.add(freePhysicalPages.pollFirst());
        }
        return list;
    }

    public static void freePhysicalPages(List<Integer> physicalPageNumberList) {
        freePhysicalPages.addAll(physicalPageNumberList);
    }

}
