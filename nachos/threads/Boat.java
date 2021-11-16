package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat {
    static BoatGrader bg;
    /**
     * 在A岛上孩子的数量
     */
    private static int children_inA;
    /**
     * 在A岛上成人的数量
     */
    private static int adult_inA;
    /**
     * 在Moloka岛上孩子的数量
     */
    private static int children_inB;
    /**
     * 在Moloka岛上成人的数量
     */
    private static int adult_inB;
    /**
     * 孩子在A岛上的条件变量
     */
    private static Condition children_condition_A;
    /**
     * 孩子在B岛上的条件变量
     */
    private static Condition children_condition_B;
    /**
     * 成人在A岛上的条件变量
     */
    private static Condition adult_condition_A;

    private static Lock lock;
    /**
     * 判断是否该成人走
     */
    private static boolean adult_can_go;
    /**
     * 判断船是否在A
     */
    private static boolean boat_A;
    /**
     * 判断目前的孩子是否是驾驶员
     */
    private static boolean isPilot;
    /**
     * 判断是否结束
     */
    private static boolean isFinish;
    static boolean is_first_go = true;
    static boolean newOne = true;
    static int ChildrenN;

    public static void selfTest() {
        BoatGrader b = new BoatGrader();

        System.out.println("\n ***Testing Boats with 2 children, 5 adults***");
        begin(1, 2, b);

    }

    public static void begin(int adults, int children, BoatGrader b) {

        //将外部生成的自动分级器存储在类变量中，以供孩子使用。
        Lib.assertTrue(children >= 2);
        Lib.assertTrue(b != null);
        ChildrenN = children;
        bg = b;

        //在此处实例化全局变量
        children_inA = children;
        adult_inA = adults;
        children_inB = 0;
        adult_inB = 0;
        lock = new Lock();
        children_condition_A = new Condition1(lock);
        children_condition_B = new Condition1(lock);
        adult_condition_A = new Condition1(lock);
        isPilot = true;
        adult_can_go = false;
        isFinish = false;
        boat_A = true;


        //创建adults个大人线程
        for (int i = 0; i < adults; i++) {
            Runnable r = new Runnable() {
                public void run() {
                    AdultItinerary();
                }
            };
            KThread t = new KThread(r);
            t.setName("Adult" + i + "Thread").fork();
        }

        //创建children个小孩线程
        for (int i = 0; i < children; i++) {
            Runnable r = new Runnable() {
                public void run() {
                    if (ChildrenN > 1) {
                        newOne = true;
                        ChildrenN--;
                    } else {
                        newOne = false; // 防止有了两个小孩就开始走的情况
                    }
                    ChildItinerary();
                }
            };
            KThread t = new KThread(r);
            t.setName("Children" + i + "Thread").fork();
        }

        KThread.yield();
    }

    static void AdultItinerary() {
        bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
        lock.acquire();

        //如果成年人不能走,则将条件变量睡眠
        if (!(adult_can_go && boat_A)) {
            adult_condition_A.sleep();
        }
        bg.AdultRowToMolokai(); // adult to B
        adult_inA--;            // 在A的成人数量-1
        adult_inB++;            // 在B的成人数量+1
        adult_can_go = false;   //这一次是成人走，下一次必须是孩子走。因为要保证B要至少有一个孩子
        boat_A = false;         //成人过去了，船也过去了。
        children_condition_B.wake(); //唤醒一个在B的孩子，将船驶回A

        lock.release();
    }

    static void ChildItinerary() {
        bg.initializeChild(); //Required for autograder interface. Must be the first thing called.

        boolean is_on_A = true;

        lock.acquire();
        // 运输没有完成
        while (!isFinish) {
            //如果这个孩子在A上
            if (is_on_A) {
                if (!boat_A || adult_can_go || newOne) { // 如果船没在A、或者该成人走了，该孩子睡眠
                    children_condition_A.sleep();
                }
                if (isPilot) { // 如果是第一个小孩，则设为舵手
                    bg.ChildRowToMolokai();
                    is_on_A = false;
                    children_inA--; //A的孩子减少一个
                    children_inB++; //B的孩子增加一个
                    isPilot = false;
                    // 他是船长，再呼唤一个孩子一起走
                    children_condition_A.wake();
                    // boat_A=false; //把船设为不在A
                    children_condition_B.sleep(); //把孩子设为molokai并且不能走
                } else { // 如果是第二个小孩，则设为游客
                    if (adult_inA == 0 && children_inA == 1)
                        isFinish = true;// 运输即将完成
                    // 如果孩子不是船长，且在A的成人不为0，那么该孩子就作为乘客走
                    if (adult_inA != 0)
                        adult_can_go = true;
                    bg.ChildRideToMolokai();
                    is_on_A = false;
                    boat_A = false;
                    children_inA--;
                    children_inB++;
                    isPilot = true; //把船开过去后，孩子在B岛都可以作舵手
                    if (!isFinish) {
                        children_condition_B.wake();
                    }
                    children_condition_B.sleep();
                }

            } else {  //如果船不在A,则孩子划船去A
                bg.ChildRowToOahu();
                is_on_A = true;
                boat_A = true;
                children_inB--;
                children_inA++;
                if (adult_inA == 0) { // 如果 在A的成人数量为0,则成人不可走,把孩子唤醒
                    adult_can_go = false;
                    children_condition_A.wake();
                } else {
                    if (adult_can_go)
                        adult_condition_A.wake();
                    else
                        children_condition_A.wake();
                }
                children_condition_A.sleep();
            }
        }
        lock.release();
    }

    static void SampleItinerary() {
// Please note that this isn't a valid solution (you can't fit
// all of them on the boat). Please also note that you may not
// have a single thread calculate a solution and then just play
// it back at the autograder -- you will be caught.
        //请注意，这不是有效的解决方案（您无法在船上全部安装）。
        //另请注意，您可能没有一个线程来计算解决方案，然后仅在自动平分机上播放它-您将被抓住。
        System.out.println("\n ***Everyone piles on the boat and goes to B***");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();
    }

}

