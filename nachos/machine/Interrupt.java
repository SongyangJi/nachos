// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.*;

import java.util.TreeSet;

/**
 * Interrupt类模拟 low-level 的中断硬件。
 * The <tt>Interrupt</tt> class emulates low-level interrupt hardware. The
 * hardware provides a method (<tt>setStatus()</tt>) to enable or disable
 * interrupts.
 *
 * <p>
 * In order to emulate the hardware, we need to keep track of all pending
 * interrupts the hardware devices would cause, and when they are supposed to
 * occur.
 *
 * <p>
 * This module also keeps track of simulated time. Time advances only when the
 * following occur:
 * <ul>
 * <li>interrupts are enabled, when they were previously disabled
 * <li>a MIPS instruction is executed
 * </ul>
 *
 * <p>
 * As a result, unlike real hardware, interrupts (including time-slice context
 * switches) cannot occur just anywhere in the code where interrupts are
 * enabled, but rather only at those places in the code where simulated time
 * advances (so that it becomes time for the hardware simulation to invoke an
 * interrupt handler).
 *
 * <p>
 * This means that incorrectly synchronized code may work fine on this hardware
 * simulation (even with randomized time slices), but it wouldn't work on real
 * hardware. But even though Nachos can't always detect when your program
 * would fail in real life, you should still write properly synchronized code.
 */
public final class Interrupt {
    /**
     * Allocate a new interrupt controller.
     *
     * @param privilege encapsulates privileged access to the Nachos
     *                  machine.
     */
    public Interrupt(Privilege privilege) {
        System.out.println("interrupt");

        this.privilege = privilege;
        privilege.interrupt = new InterruptPrivilege();

        enabled = false;
        pending = new TreeSet<>();
    }

    /**
     * Enable interrupts. This method has the same effect as
     * <tt>setStatus(true)</tt>.
     */
    public void enable() {
        setStatus(true);
    }

    /**
     * Disable interrupts and return the old interrupt state. This method has
     * the same effect as <tt>setStatus(false)</tt>.
     *
     * @return <tt>true</tt> if interrupts were enabled.
     */
    public boolean disable() {
        return setStatus(false);
    }

    /**
     * Restore interrupts to the specified status. This method has the same
     * effect as <tt>setStatus(<i>status</i>)</tt>.
     *
     * @param status <tt>true</tt> to enable interrupts.
     */
    public void restore(boolean status) {
        setStatus(status);
    }

    /**
     * Set the interrupt status to be enabled (<tt>true</tt>) or disabled
     * (<tt>false</tt>) and return the previous status. If the interrupt
     * status changes from disabled to enabled, the simulated time is advanced.
     *
     * @param status <tt>true</tt> to enable interrupts.
     * @return <tt>true</tt> if interrupts were enabled.
     */
    public boolean setStatus(boolean status) {
        boolean oldStatus = enabled;
        enabled = status;
        // 如果是从 关中断 -> 开中断, 则 tick()
        if (!oldStatus && status) {
//            System.out.println("to here tick1");
//            System.out.println(privilege.stats.totalTicks);
//            System.out.println(Thread.currentThread().getName()+"\n");
            tick(true);
        }

        return oldStatus;
    }

    /**
     * Tests whether interrupts are enabled.
     *
     * @return <tt>true</tt> if interrupts are enabled.
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Tests whether interrupts are disabled.
     *
     * @return <tt>true</tt> if interrupts are disabled.
     */
    public boolean disabled() {
        return !enabled;
    }

    /**
     *
     * 内核的硬件可以通过此方法将中断加入 pending 容器中。
     * @param when 前进的时钟滴答数
     * @param type 中断类型
     * @param handler 中断处理函数
     */
    private void schedule(long when, String type, Runnable handler) {
        Lib.assertTrue(when > 0);

        long time = privilege.stats.totalTicks + when;
        PendingInterrupt toOccur = new PendingInterrupt(time, type, handler);

        Lib.debug(dbgInt,
                "Scheduling the " + type +
                        " interrupt handler at time = " + time);

        pending.add(toOccur);
    }

    /**
     * 模拟计算机时钟的跳动。
     * 该方法会在两个地方被调用，
     * 第一个是 Interrupt 类的 setStatus 方法中，该方法会检测此时是否是从关中断到开中断，如果是，那么就会执行 tick()，
     * 第二个是在Processor 类的 run()方法中，每执行一条指令便会执行一次 tick()方法，然后调用 checkIfDue() 相当于处于指令周期的中断周期
     * @param inKernelMode 执行模式 —— 用户模式、内核模式
     */
    private void tick(boolean inKernelMode) {
        Stats stats = privilege.stats;

        if (inKernelMode) {
            stats.kernelTicks += Stats.KernelTick;
            stats.totalTicks += Stats.KernelTick;
        } else {
            stats.userTicks += Stats.UserTick;
            stats.totalTicks += Stats.UserTick;
        }

        if (Lib.test(dbgInt))
            System.out.println("== Tick " + stats.totalTicks + " ==");

        // 现在还是是开中断的
        // todo 关中断
        enabled = false;

        // 检查是否有要处理的中断
        checkIfDue();

        // todo 开中断
        enabled = true;
    }

    /**
     * 检测 pending 容器中有哪些中断是已经到了其触发中断的时间
     * 将那些到了时间的中断取出并执行其中断处理函数
     */
    private void checkIfDue() {
        long time = privilege.stats.totalTicks;

        // 在处理中断前，先屏蔽中断
        Lib.assertTrue(disabled());

        if (Lib.test(dbgInt))
            print();

        if (pending.isEmpty())
            return;

        if (pending.first().time > time)
            return;

        Lib.debug(dbgInt, "Invoking interrupt handlers at time = " + time);

        while (!pending.isEmpty() &&
                pending.first().time <= time) {
            PendingInterrupt next = pending.first();
            pending.remove(next);

            Lib.assertTrue(next.time <= time);

            if (privilege.processor != null)
                privilege.processor.flushPipe();

            Lib.debug(dbgInt, "  " + next.type);

            // 调用中断处理函数
            next.handler.run();
        }

        Lib.debug(dbgInt, "  (end of list)");
    }

    private void print() {
        System.out.println("Time: " + privilege.stats.totalTicks
                + ", interrupts " + (enabled ? "on" : "off"));
        System.out.println("Pending interrupts:");

        for (PendingInterrupt toOccur : pending) {
            System.out.println("  " + toOccur.type +
                    ", scheduled at " + toOccur.time);
        }

        System.out.println("  (end of list)");
    }

    /**
     * 挂起的中断
     */
    private class PendingInterrupt implements Comparable<PendingInterrupt> {
        PendingInterrupt(long time, String type, Runnable handler) {
            this.time = time;
            this.type = type;
            this.handler = handler;
            this.id = numPendingInterruptsCreated++;
        }

        public int compareTo(PendingInterrupt o) {

            // can't return 0 for unequal objects, so check all fields
            if (time < o.time)
                return -1;
            else if (time > o.time)
                return 1;
            else return Long.compare(id, o.id);
        }

        long time;
        String type;
        Runnable handler;

        private final long id;
    }

    private long numPendingInterruptsCreated = 0;

    private final Privilege privilege;

    private boolean enabled;
    /**
     * 存储各种 PendingInterrupt 的容器
     */
    private final TreeSet<PendingInterrupt> pending;

    private static final char dbgInt = 'i';

    private class InterruptPrivilege implements Privilege.InterruptPrivilege {
        public void schedule(long when, String type, Runnable handler) {
            Interrupt.this.schedule(when, type, handler);
        }

        public void tick(boolean inKernelMode) {
//            System.out.println("to here tick2");
            Interrupt.this.tick(inKernelMode);
        }
    }
}
