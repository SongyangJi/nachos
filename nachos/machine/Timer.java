// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

import nachos.security.*;

/**
 * Timer 类模拟硬件定时器。
 * 硬件定时器大约每 500 个时钟周期产生一个 CPU 定时器中断。
 * <p>
 * A hardware timer generates a CPU timer interrupt approximately every 500
 * clock ticks. This means that it can be used for implementing time-slicing,
 * or for having a thread go to sleep for a specific period of time.
 * <p>
 * The <tt>Timer</tt> class emulates a hardware timer by scheduling a timer
 * interrupt to occur every time approximately 500 clock ticks pass. There is
 * a small degree of randomness here, so interrupts do not occur exactly every
 * 500 ticks.
 */
public final class Timer {
    /**
     * Allocate a new timer.
     *
     * @param privilege encapsulates privileged access to the Nachos
     *                  machine.
     */
    public Timer(Privilege privilege) {
        System.out.println("timer");

        this.privilege = privilege;

        // 时钟中断
        timerInterrupt = new Runnable() {
            public void run() {
                timerInterrupt();
            }
        };

        autoGraderInterrupt = new Runnable() {
            public void run() {
                Machine.autoGrader().timerInterrupt(Timer.this.privilege,
                        lastTimerInterrupt);
            }
        };

        // 硬件定时器一初始化就要不断发出 定时的时钟中断（供抢占使用）
//        System.out.println("time cur thread : " + Thread.currentThread().getName());
        scheduleInterrupt();
    }

    /**
     * Set the callback to use as a timer interrupt handler. The timer
     * interrupt handler will be called approximately every 500 clock ticks.
     *
     * @param handler the timer interrupt handler.
     */
    public void setInterruptHandler(Runnable handler) {
        this.handler = handler;
    }

    /**
     * Get the current time.
     *
     * @return the number of clock ticks since Nachos started.
     */
    public long getTime() {
        return privilege.stats.totalTicks;
    }

    private void timerInterrupt() {
        // 再次将时钟中断加入中断容器
        scheduleInterrupt();
        scheduleAutoGraderInterrupt();

        lastTimerInterrupt = getTime();

        // 每次处理时间中断还要做的事
        if (handler != null)
            handler.run();
    }

    private void scheduleInterrupt() {
        int delay = Stats.TimerTicks;
        delay += Lib.random(delay / 10) - (delay / 20); // 加一个随机波动
        // 中断类型是 时间中断，中断处理函数是再次发出定时的时间中断
        privilege.interrupt.schedule(delay, "timer", timerInterrupt);
    }

    private void scheduleAutoGraderInterrupt() {
        privilege.interrupt.schedule(1, "timerAG", autoGraderInterrupt);
    }

    private long lastTimerInterrupt;
    //
    private final Runnable timerInterrupt;
    private final Runnable autoGraderInterrupt;

    private final Privilege privilege;
    //
    private Runnable handler = null;
}
