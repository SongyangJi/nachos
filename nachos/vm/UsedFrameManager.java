package nachos.vm;

import nachos.machine.Lib;
import nachos.machine.TranslationEntry;

import java.util.List;

/**
 * @author: SongyangJi
 * @description:
 * @since: 2021/12/5
 */
public interface UsedFrameManager {

    class EntryOfProcess {
        int pid;
        TranslationEntry translationEntry;

        public EntryOfProcess(int pid, TranslationEntry translationEntry) {
            this.pid = pid;
            this.translationEntry = translationEntry;
        }

        @Override
        public String toString() {
            return "EntryOfProcess{" +
                    "pid=" + pid +
                    ", translationEntry=" + translationEntry +
                    '}';
        }
    }


    List<Integer> cleanUp(int pid);

    void addOneFrame(EntryOfProcess entry);

    EntryOfProcess pickSwappedFrame();

    TranslationEntry access(TranslationEntry entry, boolean write);

}
