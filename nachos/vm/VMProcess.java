package nachos.vm;

import nachos.machine.*;
import nachos.threads.KThread;
import nachos.userprog.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {


    static class DiskNode {
        CoffSection section;
        int numPageOffsetInSection;

        public DiskNode(CoffSection section, int numPageOffsetInSection) {
            this.section = section;
            this.numPageOffsetInSection = numPageOffsetInSection;
        }
    }

    /**
     * Allocate a new process.
     */
    public VMProcess() {
        super();
        diskFileMap = new HashMap<>();
    }

    public VMProcess(Map<Integer, DiskNode> diskFileMap, HeapManager heapManager) {
        super();
        this.diskFileMap = diskFileMap;
        this.heapManager = heapManager;
    }

    @Override
    protected int virtualToPhysicalAddress(int vaddr, boolean writing) {
        handleTLBMiss(vaddr);
        return super.virtualToPhysicalAddress(vaddr, writing);
    }

    @Override
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
//        System.out.println("VMProcess readVirtualMemory()");
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        Lib.assertTrue(vaddr < MAX_VIRTUAL_ADDRESS, "address access error");

        byte[] memory = Machine.processor().getMemory();

        // 仅仅是地址空间的上界发生了变化
        int amount = Math.min(length, MAX_VIRTUAL_ADDRESS - vaddr);

        int base = vaddr;
        int paddr;
        for (int i = offset; i < amount + offset; i++) {
            vaddr = base + i;
            paddr = virtualToPhysicalAddress(vaddr, false);
            if (paddr == -1) return 0;
            data[i] = memory[paddr];
        }
        return amount;
    }


    @Override
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
//        System.out.println("VMProcess  writeVirtualMemory()");

        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);


        byte[] memory = Machine.processor().getMemory();

        if (vaddr < 0 || vaddr >= MAX_VIRTUAL_ADDRESS)
            return 0;

        // 仅仅是地址空间的上界发生了变化
        int amount = Math.min(length, MAX_VIRTUAL_ADDRESS - vaddr);

        int base = vaddr;
        int paddr;
        for (int i = offset; i < amount + offset; i++) {
            vaddr = base + i;
            paddr = virtualToPhysicalAddress(vaddr, true);
            if (paddr == -1) return 0;
            memory[paddr] = data[i];
        }
        return amount;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        // 刷新 TLB
//        System.out.println("刷新 TLB");
        Machine.processor().flushTLB();
    }


    private int getOneFrameIfNeedSwap() {
//        System.out.println("to getOneFrameIfNeedSwap");
        if (!FreeFrameManager.hasFreeFrame()) { // 无空闲内存，挑一个牺牲帧，并将其写入 swap
            pickAndSwap();
        }
        int x = FreeFrameManager.allocateOnePhysicalPage();
        return x; // 空闲帧号
    }

    @Override
    protected boolean initPageTable() {
//        System.out.println("VMProcess initPageTable()");
        // todo 初始化页表
        pageTable = new TranslationEntry[MAX_VIRTUAL_PAGES];

        // 直接给参数分配一页
        int ppn = getOneFrameIfNeedSwap();
        if (ppn == -1) return false;
        TranslationEntry entry = new TranslationEntry(this.argPageNumber, ppn, true, false, false, true);
        pageTable[this.argPageNumber] = entry;
        VMKernel.usedFrameManager.addOneFrame(new UsedFrameManager.EntryOfProcess(this.pid, entry));

        // 初始化 堆管理器
        heapManager = new HeapManager(this.numPages, MAX_VIRTUAL_PAGES);

        return true;
    }


    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return <tt>true</tt> if successful.
     */
    @Override
    protected boolean loadSections() {
        // todo 这一步仅仅需要记录下磁盘地址与内存地址的映射关系就可以了
        // load sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                    + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;
                diskFileMap.put(vpn, new DiskNode(section, i));
            }
        }

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        super.unloadSections();
    }


    /**
     * 挑选一个牺牲帧，并修改PageTable、TLB, 最后释放此帧
     */
    private void pickAndSwap() {
//        System.out.println("to pickAndSwap");
        UsedFrameManager.EntryOfProcess victim = VMKernel.usedFrameManager.pickSwappedFrame();
        TranslationEntry entry = victim.translationEntry;
        int pid = victim.pid;
        int vpn = entry.vpn;
        int ppn = entry.ppn;
        // 写入swap
        SwapManager.writeIntoSwapSpaceFromMemory(pid, vpn, ppn);
//        if (victim.translationEntry.dirty) {
//
//        }
        // 页表某个entry失效
        entry.valid = false;
        // TLB失效
        Machine.processor().invalidTLBEntry(ppn);
        // 帧释放
        FreeFrameManager.freePhysicalPages(ppn);
    }

    protected void handleTLBMiss(int badVAddr) {
//        System.out.println("handleTLBMiss...");

        int vpn = Processor.pageFromAddress(badVAddr);

        TranslationEntry newEntry;

        if (pageTable[vpn] != null && pageTable[vpn].valid) { // 错误页已经加载到物理内存中，只不过TLB中没有
            newEntry = pageTable[vpn];
//            System.out.println("错误页已经加载到物理内存中，只不过TLB中没有");
        } else {  // 初始加载 或者是从 swap区 加载

            // incr pageFault
            Machine.processor().incrPageFault();

            int ppn = getOneFrameIfNeedSwap();

            Lib.assertTrue(ppn != -1, "no available physical frame"); // 此时必定有可用的物理内存


            if (!SwapManager.existInSwap(this.pid, vpn)) {
                if (diskFileMap.containsKey(vpn)) { // 加载 coff
                    newEntry = pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
                    DiskNode diskNode = diskFileMap.get(vpn);
                    CoffSection section = diskNode.section;
                    int i = diskNode.numPageOffsetInSection;
                    if (section.isReadOnly()) {
                        newEntry.readOnly = true;
                    }
                    section.loadPage(i, ppn);

                } else {  // 加载stack or heap
                    // todo 目前只需如此
                    newEntry = pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
                }

            } else {
                newEntry = pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
                SwapManager.readIntoMemoryFromSwap(this.pid, vpn, ppn);
            }
            // todo 当从加载到物理内存时，其实此时 cow 已经失效了，也就是说父子进程并不共享某个物理帧了
            newEntry.cow = false;
            VMKernel.usedFrameManager.addOneFrame(new UsedFrameManager.EntryOfProcess(this.pid, newEntry)); // 将这一帧加入
        }
        // 更新 TLB
        int number = Machine.processor().pickTLBEntry();
//        System.out.println("pid " + pid + "  new entry " + newEntry);
        Machine.processor().writeTLBEntry(number, newEntry);
    }

    @Override
    protected int handleMalloc(int size) {
//        System.out.println("to malloc");
        int pages = size / pageSize;
        if (size % pageSize > 0) ++pages;
        int base = heapManager.malloc(pages);
        if (base < 0) return -1;
//        System.out.println("base address : " + base * pageSize);
        return base * pageSize;
    }

    @Override
    protected int handleFree(int ptr) {
//        System.out.println("to free");
        heapManager.free(ptr / pageSize);
        return 0;
    }

    @Override
    protected int handleExit(int status) {
//        System.out.println("VMProcess exit()");
        this.exitStatus = status;

        // todo 删除 UsedFrameManager 中的记录
        List<Integer> physicalPages = VMKernel.usedFrameManager.cleanUp(pid);
        // todo 释放帧表
        FreeFrameManager.freePhysicalPages(physicalPages);
        // todo 删除 SwapManager 中的记录
        SwapManager.cleanUp(pid);

        KThread.finish();
        return 0;
    }


    @Override
    protected UserProcess forkAndCopyAddressSpace() {
        VMProcess childProcess = new VMProcess(this.diskFileMap, this.heapManager.cloneMe());
        childProcess.pageTable = new TranslationEntry[MAX_VIRTUAL_PAGES];
        for (int i = 0; i < numPages; i++) {
            if (this.pageTable[i] != null && this.pageTable[i].valid) {
                this.pageTable[i].cow = true;
            }
            if (this.pageTable[i] != null) {
                childProcess.pageTable[i] = new TranslationEntry(this.pageTable[i]);
            }
        }
        // todo
        SwapManager.cloneFather(this.pid, childProcess.pid);
        return childProcess;
    }

    /**
     * 处理写时复制
     */
    protected void handleCoW(int address) {
//        System.out.println("VM handleCoW()");
        int vpn = Processor.pageFromAddress(address);
        Lib.assertTrue(this.pageTable[vpn].valid, "当前页不可用!");
        int old_ppn = this.pageTable[vpn].ppn;
        int new_ppn = getOneFrameIfNeedSwap();
        // 写时复制页面
        Processor.copy(old_ppn, new_ppn);
        // 指向新的一帧
        this.pageTable[vpn].ppn = new_ppn;
        // 加入使用帧跟踪
        VMKernel.usedFrameManager.addOneFrame(new UsedFrameManager.EntryOfProcess(this.pid, this.pageTable[vpn]));
        // 父子不再共享
        this.pageTable[vpn].cow = false;
        // 原先的 ppn 失效
        Machine.processor().invalidTLBEntry(old_ppn);
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

        switch (cause) {
            case Processor.exceptionSyscall:
                // Processor.regV0 中存储系统调用号
                // Processor.regAX 存储系统调用参数
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0),
                        processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2),
                        processor.readRegister(Processor.regA3)
                );
                // 将系统调用的结果写回寄存器
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            case Processor.exceptionTLBMiss:
                int badVAddr = processor.readRegister(Processor.regBadVAddr);
                handleTLBMiss(badVAddr);
                break;

            case Processor.exceptionCopyOnWrite:
                int address = processor.readRegister(Processor.regBadVAddr);
                handleCoW(address);
                break;


            default:
                System.err.println("cause : " + Processor.exceptionNames[cause]);
                Lib.debug(dbgProcess, "Unexpected exception: " +
                        Processor.exceptionNames[cause]);
                Lib.assertNotReached("process-" + pid + " unexpected exception");
        }
    }

    protected final Map<Integer, DiskNode> diskFileMap;

    protected HeapManager heapManager;

    private static final int pageSize = Processor.pageSize;
    /**
     * 假定虚拟内存空间有 128 MB
     */
    public static final int MAX_VIRTUAL_PAGES = 128 * 1024;

    public static final int MAX_VIRTUAL_ADDRESS = MAX_VIRTUAL_PAGES * pageSize;

    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
}