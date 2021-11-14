package nachos.threads;


public interface Condition {

    void sleep();

    void wake();

    void wakeAll();

}
