package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.TranslationEntry;

import java.util.*;

/**
 * @author: SongyangJi
 * @description:
 * @since: 2021/12/8
 */
public class LruUsedFrameManager implements UsedFrameManager {


    protected final LinkedHashMap<Integer, EntryOfProcess> linkedHashMap;

    public LruUsedFrameManager() {
        linkedHashMap = new LinkedHashMap<>(8, 0.75F, true);
    }


    @Override
    public List<Integer> cleanUp(int pid) {
        List<Integer> list = new ArrayList<>();
        Iterator<Map.Entry<Integer, EntryOfProcess>> iterator = linkedHashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, EntryOfProcess> next = iterator.next();
            if (next.getValue().pid == pid) {
                iterator.remove();
                list.add(next.getValue().translationEntry.ppn);
            }
        }
        return list;
    }

    @Override
    public void addOneFrame(EntryOfProcess entry) {
        int ppn = entry.translationEntry.ppn;
        linkedHashMap.remove(ppn);
        linkedHashMap.put(ppn, entry);
    }

    @Override
    public EntryOfProcess pickSwappedFrame() {
        Lib.assertTrue(!linkedHashMap.isEmpty());
        Map.Entry<Integer, EntryOfProcess> next = linkedHashMap.entrySet().iterator().next();// 拿到第一个
        EntryOfProcess entry = next.getValue();
        int ppn = next.getKey();
        Lib.assertTrue(linkedHashMap.remove(ppn) != null);
        return entry;
    }


    @Override
    public TranslationEntry access(TranslationEntry entry, boolean write) {
        Lib.assertTrue(linkedHashMap.containsKey(entry.ppn));
        TranslationEntry translationEntry = linkedHashMap.get(entry.ppn).translationEntry;
        Lib.assertTrue(translationEntry != null);
        translationEntry.used = true;
        // todo
        translationEntry.dirty = write;
        return translationEntry;
    }
}
