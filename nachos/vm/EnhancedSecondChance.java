package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.TranslationEntry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: SongyangJi
 * @description:
 * @since: 2021/12/8
 */
public class EnhancedSecondChance implements UsedFrameManager {

    private final LinkedList<EntryOfProcess> usedPhysicalPages;


    public EnhancedSecondChance() {
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


    private int pickHelp(boolean use, boolean dirty) {
        int idx = 0;
        for (int i = 0; i < usedPhysicalPages.size(); i++) {
            TranslationEntry entry = usedPhysicalPages.get(i).translationEntry;
            if (entry.used == use && entry.dirty == dirty) {
                idx = i;
                return idx;
            }
        }
        return idx;
    }

    /**
     * @return 和 FIFO的区别主要在这里
     */
    @Override
    public EntryOfProcess pickSwappedFrame() {
//        if(true) {
//            return usedPhysicalPages.pollFirst();
//        }
        // (used, dirty) (x,x)
        // 依次检查 00  10 01 11
        int idx;
        idx = pickHelp(false, false);
        if (idx >= 0) {
            return usedPhysicalPages.remove(idx);
        }
        idx = pickHelp(true, false);
        if (idx >= 0) {
            return usedPhysicalPages.remove(idx);
        }
        idx = pickHelp(false, true);
        if (idx >= 0) {
            return usedPhysicalPages.remove(idx);
        }
        return usedPhysicalPages.pollFirst();
    }

    @Override
    public TranslationEntry access(TranslationEntry entry, boolean write) {
        TranslationEntry target = null;
        for (EntryOfProcess entryOfProcess : usedPhysicalPages) {
            if (entryOfProcess.translationEntry.ppn == entry.ppn) {
                if (target != null) {
                    throw new RuntimeException("target != null");
                }
                target = entryOfProcess.translationEntry;
            }
        }

        Lib.assertTrue(target != null);

        target.used = true;
        // todo
        target.dirty = write;
        return target;
    }
}
