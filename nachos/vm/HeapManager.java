package nachos.vm;

import java.util.HashMap;

/**
 * @author: SongyangJi
 * @description:
 * @since: 2021/12/7
 */
public class HeapManager extends IntervalManager implements Cloneable {

    private HashMap<Integer, Integer> baseAndLimit;

    public final int MAX_AVAILABLE_PAGE;

    public HeapManager(int l, int r) {
        super(l, r);
        MAX_AVAILABLE_PAGE = r - l;
        baseAndLimit = new HashMap<>();
    }

    @Override
    public int malloc(int size) {
        int base = super.malloc(size);
        baseAndLimit.put(base, size);
        return base;
    }

    public void free(int base) {
        Integer size = baseAndLimit.get(base);
        if (size == null) return;
        free(base, size);
    }

    @Override
    public HeapManager cloneMe() {
        try {
            return (HeapManager) clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        HeapManager heapManager = (HeapManager) super.clone();
        heapManager.baseAndLimit = new HashMap<>(this.baseAndLimit);
        return heapManager;
    }

    public static void main(String[] args) {
        HeapManager heapManager = new HeapManager(0, 100);
        HeapManager heapManager1 = heapManager.cloneMe();
        System.out.println(heapManager);
        System.out.println(heapManager1);

        System.out.println(heapManager.baseAndLimit == heapManager1.baseAndLimit);
    }
}
