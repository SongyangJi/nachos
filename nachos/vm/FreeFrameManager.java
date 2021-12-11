package nachos.vm;

import nachos.machine.Machine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: SongyangJi
 * @description:
 * @since: 2021/12/5
 */
public class FreeFrameManager {

    // todo 同步访问
    // 空闲帧列表
    private static final LinkedList<Integer> freePhysicalPages;



    static {
        freePhysicalPages = new LinkedList<>();
        int numPhysPages = Machine.processor().getNumPhysPages();
        for (int i = 0; i < numPhysPages; i++) {
            freePhysicalPages.add(i);
        }
    }

    /**
     * @return 可用帧
     */
    public static int numFreePhysicalPages() {
        return freePhysicalPages.size();
    }


    public static boolean hasFreeFrame() {
        return !freePhysicalPages.isEmpty();
    }


    public static Integer allocateOnePhysicalPage() {
        if (freePhysicalPages.size() < 1) {
            return -1;
        }
        return freePhysicalPages.pollFirst();
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

    public static void freePhysicalPages(int ppn) {
        freePhysicalPages.add(ppn);
    }



}
