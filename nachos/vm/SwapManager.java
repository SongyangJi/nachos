package nachos.vm;

import nachos.machine.*;
import nachos.threads.ThreadedKernel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: SongyangJi
 * @description:
 * @since: 2021/12/5
 */
public class SwapManager {

    private static final Map<PidVpn, Integer> pageMapToOffset;

    // todo swap 文件存储的页号偏移, 目前实现自增即可
    private static int pageOffset = 0;
    private static final int pageSize = Processor.pageSize;


    static {
        pageMapToOffset = new HashMap<>();
        // todo 为测试方便，每次启动机器，先将就的 swap 文件删除
        ThreadedKernel.fileSystem.remove("swap");
    }

    public static void cloneFather(int fid, int cid) {
        Map<PidVpn, Integer> child = new HashMap<>();
        for (Map.Entry<PidVpn, Integer> entry : pageMapToOffset.entrySet()) {
            if (entry.getKey().pid == fid) {
                int vpn = entry.getKey().vpn;
                int fileOffset = entry.getValue();
                PidVpn pidVpn = new PidVpn(cid, vpn);
                child.put(pidVpn, fileOffset);
            }
        }
        pageMapToOffset.putAll(child);
    }

    public static void cleanUp(int pid) {
        pageMapToOffset.entrySet().removeIf(next -> next.getKey().pid == pid);
    }

    public static boolean existInSwap(int pid, int vpn) {
        return pageMapToOffset.containsKey(new PidVpn(pid, vpn));
    }

    public static void readIntoMemoryFromSwap(int pid, int vpn, int ppn) {
//        System.out.println("pid,vpn,ppn  : " + pid + " " + vpn + " " + ppn);
        PidVpn pidVpn = new PidVpn(pid, vpn);
        Lib.assertTrue(pageMapToOffset.containsKey(pidVpn));
        int fileOffset = pageMapToOffset.get(pidVpn);
        byte[] buffer = new byte[pageSize];
        OpenFile swap = ThreadedKernel.fileSystem.open("swap", true);
        Lib.assertTrue(swap != null, "swap is null !");
        swap.read(fileOffset, buffer, 0, pageSize);
        swap.close();
        System.arraycopy(buffer, 0, Machine.processor().getMemory(), ppn * pageSize, pageSize);
        // 删除记录
        pageMapToOffset.remove(pidVpn);
    }

    public static void writeIntoSwapSpaceFromMemory(int pid, int vpn, int ppn) {
//        System.out.println("pid, vpn, ppn  :  " + pid + " " + vpn + " " + ppn);
        OpenFile swap = ThreadedKernel.fileSystem.open("swap", true);
        int fileOffset = (pageOffset++) * pageSize;
        int len = swap.write(fileOffset, Machine.processor().getMemory(), ppn * pageSize, pageSize);
        Lib.assertTrue(len == pageSize, "write swap failure");
        swap.close();
        PidVpn pidVpn = new PidVpn(pid, vpn);
        pageMapToOffset.put(pidVpn, fileOffset);
    }


    public static class PidVpn {
        int pid;
        int vpn;

        public PidVpn(int pid, int vpn) {
            this.pid = pid;
            this.vpn = vpn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PidVpn pidVpn = (PidVpn) o;

            if (pid != pidVpn.pid) return false;
            return vpn == pidVpn.vpn;
        }

        @Override
        public int hashCode() {
            int result = pid;
            result = 31 * result + vpn;
            return result;
        }

        @Override
        public String toString() {
            return "PidVpn{" +
                    "pid=" + pid +
                    ", vpn=" + vpn +
                    '}';
        }
    }


}
