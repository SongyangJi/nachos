package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.io.EOFException;
import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
        fileOperations = FileOperations.newFileOperations();
        childProcessMap = new HashMap<>();
    }

    private int virtualToPhysicalAddress(int vaddr) {
        int vpn = Processor.pageFromAddress(vaddr);
        int offset = Processor.offsetFromAddress(vaddr);
        return (pageTable[vpn].ppn * pageSize) + offset;
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args)) {
            return false;
        }
        // load 成功才自增pid计数器
        pid = ++pidCounter;
        uThread = new UThread(this);
        uThread.setName(name).fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
        // todo
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     * 在上下文切换后恢复此进程的状态。 由UThread.restoreState()调用。
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param vaddr     the starting virtual address of the null-terminated
     *                  string.
     * @param maxLength the maximum number of characters in the string,
     *                  not including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was
     * found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }


    private String charPointerToString(int pointer) {
        final int MAX_SIZE = 256;
        return readVirtualMemoryString(pointer, MAX_SIZE);
    }

    private int intPointerToInt(int pointer) {
        byte[] bytes = new byte[4];
        int numByte = readVirtualMemory(pointer, bytes);
        if (numByte != 4) return -1;
        return Lib.bytesToInt(bytes, 0);
    }

    private String[] argvToStringArray(int argc, int argv) {
        String[] res = new String[argc];
        for (int i = 0; i < argc; i++) {
            int p = argv + 4 * i;
            int charPointer = intPointerToInt(p);
            if (charPointer == -1) return null;
            String s = charPointerToString(charPointer);
            res[i] = s;
        }
//        System.out.println("argv :" + Arrays.toString(res));
        return res;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data  the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to read.
     * @param data   the array where the data will be stored.
     * @param offset the first byte to write in the array.
     * @param length the number of bytes to transfer from virtual memory to
     *               the array.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
                                 int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
//        if (vaddr < 0 || vaddr >= memory.length)
//            return 0;
//
        int amount = Math.min(length, memory.length - vaddr);
//        System.arraycopy(memory, vaddr, data, offset, amount);
//
        // todo
        int base = vaddr;
        int paddr;
        for (int i = offset; i < amount + offset; i++) {
            vaddr = base + i;
            paddr = virtualToPhysicalAddress(vaddr);
            data[i] = memory[paddr];
        }
        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data  the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to write.
     * @param data   the array containing the data to transfer.
     * @param offset the first byte to transfer from the array.
     * @param length the number of bytes to transfer from the array to
     *               virtual memory.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
                                  int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        // for now, just assume that virtual addresses equal physical addresses
        if (vaddr < 0 || vaddr >= memory.length)
            return 0;

        int amount = Math.min(length, memory.length - vaddr);
//        System.arraycopy(data, offset, memory, vaddr, amount);

        int base = vaddr;
        int paddr;
        for (int i = offset; i < amount + offset; i++) {
            vaddr = base + i;
            paddr = virtualToPhysicalAddress(vaddr);
            memory[paddr] = data[i];
        }
        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        } catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        // todo 初始化页表
        if (numPages > UserKernel.numFreePhysicalPages()) return false;
        pageTable = new TranslationEntry[numPages];
        List<Integer> list = UserKernel.allocatePhysicalPages(numPages);
//        System.out.println("空闲帧列表：" + list);
        Lib.assertTrue(list != null && list.size() == numPages);
        for (int i = 0; i < numPages; i++) {
            int ppn = list.get(i);
            pageTable[i] = new TranslationEntry(i, ppn, true, false, false, false);
        }


        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (byte[] bytes : argv) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, bytes) == bytes.length);
            stringOffset += bytes.length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[]{0}) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        // load sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;

                // for now, just assume virtual addresses = physical addresses
//                section.loadPage(i, vpn);

                // todo vpn->ppn
                TranslationEntry entry = pageTable[vpn];
                int ppn = entry.ppn;
                if (section.isReadOnly()) {
                    entry.readOnly = true;
                }
                section.loadPage(i, ppn);
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    }

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < Processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {

        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }


    private int handleExit(int status) {
//        System.out.println("status : " + status);
        this.exitStatus = status;
        // todo 释放帧表
        ArrayList<Integer> physicalPages = new ArrayList<>();
        for (TranslationEntry entry : pageTable) {
            physicalPages.add(entry.ppn);
        }
        UserKernel.freePhysicalPages(physicalPages);

        KThread.finish();
        return 0;
    }

    // int exec(char *file, int argc, char *argv[]);
    private int handleExec(int filePointer, int argc, int argvPointer) {
        String fileName = charPointerToString(filePointer);
        String[] argv = argvToStringArray(argc, argvPointer);
        if (fileName == null || argv == null) {
            return -1;
        }
        UserProcess userProcess = UserProcess.newUserProcess();
        if (userProcess.execute(fileName, argv)) {
            childProcessMap.put(userProcess.pid, userProcess);
            return userProcess.pid;
        }
        return -1;
    }

    private UserProcess forkProcess() {
        UserProcess userProcess = new UserProcess();
        // 复制页表
        userProcess.pageTable = new TranslationEntry[numPages];
        for (int i = 0; i < numPages; i++) {
            userProcess.pageTable[i] = new TranslationEntry(this.pageTable[i]);
        }

        {
            // 申请空闲帧
            List<Integer> list = UserKernel.allocatePhysicalPages(numPages);
            if (list == null) {
                System.err.println("fatal");
                return null;
            }
//            System.out.println("fork 空闲帧列表 : " + list);
            for (int i = 0; i < numPages; i++) {
                userProcess.pageTable[i].ppn = list.get(i);
            }

            //物理帧的复制
            for (int i = 0; i < numPages; i++) {
                Processor.copy(this.pageTable[i].ppn, userProcess.pageTable[i].ppn);
            }
        }


        // 寄存器组的复制
        int[] copyRegisters = Arrays.copyOf(Processor.currentRegisters(), Processor.numUserRegisters);
        // 子进程的返回值是 0
        copyRegisters[Processor.regV0] = 0;
//        System.out.println("#1 当前寄存器组为 ： " + Arrays.toString(copyRegisters));


        userProcess.coff = this.coff;
        userProcess.numPages = this.numPages;
        userProcess.initialPC = this.initialPC;
        userProcess.initialSP = this.initialSP;
        userProcess.argc = this.argc;
        userProcess.argv = this.argv;
        userProcess.pid = ++pidCounter;


        UThread uThread = new UThread(userProcess, copyRegisters);
        userProcess.uThread = uThread;
        uThread.setName(this.uThread.getName() + "-fork");
        // todo
        uThread.fork();

        return userProcess;
    }

    // 自己实现的 fork 系统调用
    private int handleFork() {
        UserProcess childProcess = forkProcess();
        if (childProcess == null) {
            return -1;
        }
        int pid = childProcess.pid;
        childProcessMap.put(pid, childProcess);
        return pid;
    }

    // int join(int processID, int *status);
    private int handleJoin(int pid, int statusPointer) {
        if (childProcessMap.containsKey(pid)) {
            UserProcess childUserProcess = childProcessMap.get(pid);
            // todo
            childUserProcess.uThread.join();

            childProcessMap.remove(pid);
            // 被子进程唤醒之后
            int exitStatus = childUserProcess.exitStatus;
            // 写入指针指向处
            // 写入指针指向处
            writeVirtualMemory(statusPointer, Lib.bytesFromInt(exitStatus));
            return childUserProcess.exitStatus == 0 ? 1 : 0;
        }
        return -1;
    }




    /*
     * 文件相关的5个系统调用
     */


    /**
     * 尝试打开命名磁盘文件，如果该文件不存在，则创建该文件，
     *
     * @return 可用于访问该文件的文件描述符
     */
    private int handleCreate(int namePointer) {
        String name = charPointerToString(namePointer);
        if (name == null) return -1;
        return fileOperations.createFile(name);
    }

    private int handleOpen(int namePointer) {
        String name = charPointerToString(namePointer);
        if (name == null) return -1;
        return fileOperations.openFile(name);
    }

    private int handleRead(int fileDescriptor, int bufferVAddress, int count) {
        byte[] bytes = new byte[count];
        int cnt = fileOperations.readFile(fileDescriptor, bytes, count);
        if (cnt == -1) return -1;
        // 再写到内存里
        writeVirtualMemory(bufferVAddress, bytes, 0, cnt);
        return cnt;
    }

    private int handleWrite(int fileDescriptor, int bufferVAddress, int count) {
        byte[] bytes = new byte[count];
        int cnt = readVirtualMemory(bufferVAddress, bytes);
//        String s = new String(bytes, 0, cnt);
        return fileOperations.writeFile(fileDescriptor, bytes, cnt);
    }

    private int handleClose(int fileDescriptor) {
        return fileOperations.closeFile(fileDescriptor);
    }

    private int handleRemove(int namePointer) {
        String name = charPointerToString(namePointer);
        if (name == null) return -1;
        return fileOperations.unlinkFile(name);
    }


    private static final int
            syscallHalt = 0,
            syscallExit = 1,
            syscallExec = 2,
            syscallJoin = 3,
            syscallCreate = 4,
            syscallOpen = 5,
            syscallRead = 6,
            syscallWrite = 7,
            syscallClose = 8,
            syscallUnlink = 9,
            syscallMmap = 10,
            syscallConnect = 11,
            syscallAccept = 12,
            syscallFork = 13;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     * 								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     * 								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     *
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    // todo 其他的系统调用
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
//        System.out.println("syscall : " + syscall);
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallExit:
                return handleExit(a0);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);

            case syscallCreate:  // todo 下面几个几个文件读写相关的系统调用
                return handleCreate(a0);
            case syscallOpen:
                return handleOpen(a0);
            case syscallRead:
                return handleRead(a0, a1, a2);
            case syscallWrite:
                return handleWrite(a0, a1, a2);
            case syscallClose:
                return handleClose(a0);
            case syscallUnlink:
                return handleRemove(a0);

            case syscallFork:
                return handleFork();


            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();
//        System.out.println("cause " + cause);

        switch (cause) {
            case Processor.exceptionSyscall:
                // Processor.regV0 中存储系统调用号
                // Processor.regAX 存储系统调用参数
                int syscall = processor.readRegister(Processor.regV0);
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0),
                        processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2),
                        processor.readRegister(Processor.regA3)
                );
                // 将系统调用的结果写回寄存器
                processor.writeRegister(Processor.regV0, result);
                // todo 真实的机器应该是保存用户进程的PC, 然后系统调用处理完
                processor.advancePC();
                if (syscall == 13) {
//                    System.out.println("to fork syscall");
//                    KThread.finish(); // todo
//                    System.out.println("#2 当前寄存器组为 ： " + Arrays.toString(Processor.currentRegisters()));
                }
                // todo start.s 中 syscall 后的 jump $31 与 process.advancePC(); 有何关系
                break;

            default:
                Lib.debug(dbgProcess, "Unexpected exception: " +
                        Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    /**
     * The program being run by this process.
     */
    protected Coff coff;

    /**
     * This process's page table.
     */
    protected TranslationEntry[] pageTable;
    /**
     * The number of contiguous pages occupied by the program.
     */
    protected int numPages;

    /**
     * The number of pages in the program's stack.
     */
    protected final int stackPages = 8;

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    private int exitStatus = 0;

    // associated UThread
    private UThread uThread;

    // 文件的操作实例
    private final FileOperations fileOperations;

    private int pid;

    // pid 计数器
    private static int pidCounter = 0;

    private final Map<Integer, UserProcess> childProcessMap;

}
