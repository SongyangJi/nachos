package nachos.vm;


/**
 * @author: SongyangJi
 * @description:
 * @since: 2021/12/5
 */
public class IntervalManager implements Cloneable {

    public static class Interval {
        int l, r;
        Interval next;

        @Override
        public String toString() {
            return this.hashCode()+": [" + l + "," + r + ")";
        }

        public Interval(int l, int r) {
            this.l = l;
            this.r = r;
        }

        public Interval(Interval node) {
            this.l = node.l;
            this.r = node.r;
            this.next = null;
        }

        public Interval() {
        }
    }

    private Interval intervalHead;

    public IntervalManager(int l, int r) {
        this.intervalHead = new Interval(l, r);
    }

    /**
     * @param size 区间大小
     * @return 区间起始点, -1表示失败
     */
    public int malloc(int size) {
        Interval pre = null, cur = intervalHead;
        while (cur != null) {
            if (cur.r - cur.l >= size) {
                int base = cur.l;
                cur.l += size;
                if (cur.l == cur.r) {
                    if (pre == null) {
                        intervalHead = null;
                    } else {
                        pre.next = cur.next; // 删除这个节点
                    }

                }
                return base;
            }
            pre = cur;
            cur = cur.next;
        }
        return -1;
    }

    public void free(int base, int size) {
        int l = base, r = base + size;
        if (intervalHead == null) {
            intervalHead = new Interval(l, r);
            return;
        }
        Interval pre = null, cur = intervalHead;
        while (cur != null) {
            if (r <= cur.l) {
                if (r == cur.l) {
                    cur.l = l;
                    if (pre != null && cur.l == pre.r) {
                        pre.r = cur.r;
                        pre.next = cur.next; // 删除 cur
                    }
                } else {
                    if (pre != null) {
                        if (pre.r == l) {
                            pre.r = r;
                        } else {
                            Interval interval = new Interval(l, r);
                            interval.next = cur;
                            pre.next = interval;
                        }
                    } else {
                        intervalHead = new Interval(l, r);
                        intervalHead.next = cur;
                    }
                }
                return; // end
            }
            pre = cur;
            cur = cur.next;
        }
        if (pre.r == l) {
            pre.r = r;
        } else {
            pre.next = new Interval(l, r);
        }
    }


    public void print() {
        if (intervalHead == null) {
            System.out.println("empty");
            return;
        }
        Interval node = intervalHead;
        while (node != null) {
            System.out.print(node + " -> ");
            node = node.next;
        }
        System.out.println();
    }

    public IntervalManager cloneMe() {
        try {
            return (IntervalManager) clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        IntervalManager manager = (IntervalManager) super.clone();
        // 链表的深复制

        Interval dump = new Interval(), pre = dump;
        Interval cur = this.intervalHead;

        while (cur != null) {
            pre.next = new Interval(cur);
            pre = pre.next;
            cur = cur.next;
        }
        manager.intervalHead = dump.next;
        return manager;
    }

    public static void main(String[] args) throws CloneNotSupportedException {
        IntervalManager manager = new IntervalManager(0, 100);
        int base1 = manager.malloc(10);
//        manager.print();
        int base2 = manager.malloc(60);
//        manager.print();
        int base3 = manager.malloc(30);
//        manager.print();
        manager.free(base1, 10);
//        manager.print();
        manager.free(base3, 30);


        IntervalManager clone = (IntervalManager) manager.clone();
        manager.print();
        clone.print();
    }
}
