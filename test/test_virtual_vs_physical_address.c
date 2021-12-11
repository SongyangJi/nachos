/**
 * @Author: 吉松阳
 * @Date: 2021/11/22
 * @Description:
 */

#include "stdio.h"


static int static_x = 1;

// do something to make preemptive possible
void work_boring() {
    int i, x = 0;
    for (i = 0; i < 1000000; i++) {
        x += 2;
    }
}

int main() {
    printf("test syscall fork\n");

    int local_y = 2;

    int pid = fork();
    if (pid < 0) {
        printf("fork failed\n");
        exit(1);
    }
//    printf("now pid is %d\n", pid);
    if (pid == 0) {
        printf("this is in child process, child process will do something\n");

        printf("first x is %d, address is %d\n", static_x, &static_x);
        static_x = 2;
        printf("then x is %d, address is %d\n", static_x, &static_x);

        printf("first y is %d, address is %d\n", local_y, &local_y);
        local_y = 3;
        printf("then y is %d, address is %d\n", local_y, &local_y);

        work_boring();

        printf("child process will exit...\n");
        exit(0);
    } else {
        int status = -2;
        join(pid, &status);
        printf("this is in father process, father process will do something\n");

        printf("now x is %d, address is %d\n", static_x, &static_x);
        printf("now y is %d, address is %d\n", local_y, &local_y);

        work_boring();

        printf("child process %d exit with status %d\n", pid, status);
        printf("father process will exit...\n");
        halt();
    }
}
