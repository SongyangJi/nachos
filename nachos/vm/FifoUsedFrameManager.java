package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.TranslationEntry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author: SongyangJi
 * @description:
 * @since: 2021/12/5
 */
public class FifoUsedFrameManager implements UsedFrameManager {


    private final LinkedList<EntryOfProcess> usedPhysicalPages;

    public FifoUsedFrameManager() {
        this.usedPhysicalPages = new LinkedList<>();
    }

    @Override
    public List<Integer> cleanUp(int pid) {
        List<Integer> freeList = new ArrayList<>();
        for (EntryOfProcess entry : usedPhysicalPages) {
            if (entry.pid == pid) {
                freeList.add(entry.translationEntry.ppn);
            }
        }
        usedPhysicalPages.removeIf(entry -> entry.pid == pid);
        return freeList;
    }

    @Override
    public void addOneFrame(EntryOfProcess entry) {
        int ppn = entry.translationEntry.ppn;
        usedPhysicalPages.removeIf(entryOfProcess -> entryOfProcess.translationEntry.ppn == ppn); // todo 防止重复添加
        usedPhysicalPages.add(entry);
    }

    @Override
    public EntryOfProcess pickSwappedFrame() {
        Lib.assertTrue(!usedPhysicalPages.isEmpty());
        return usedPhysicalPages.pollFirst();
    }

    @Override
    public TranslationEntry access(TranslationEntry entry, boolean write) {
        TranslationEntry target = null;
        for (EntryOfProcess entryOfProcess : usedPhysicalPages) {
            if (entryOfProcess.translationEntry.ppn == entry.ppn) {
                if (target != null) {
//                    System.out.println("ppn is " + entry.ppn);
//                    System.out.println(usedPhysicalPages);
                    throw new RuntimeException("target != null");
                }
                target = entryOfProcess.translationEntry;
            }
        }

        Lib.assertTrue(target != null);

        target.used = true;
        target.dirty = write;
        return target;
    }
}
