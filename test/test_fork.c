/**
 * @Author: 吉松阳
 * @Date: 2021/11/22
 * @Description: 
 */

#include "stdio.h"

int main() {
    printf("test syscall fork\n");

    int pid = fork();
    if (pid < 0) {
        printf("fork failed\n");
        exit(1);
    }
//    printf("now pid is %d\n", pid);
    if (pid == 0) {
        printf("this is in child process, child process will do something\n");
        int i, x = 0;
        for (i = 0; i < 1000000; i++) {
            x += 2;
        }
        printf("child process will exit...\n");
        exit(0);
    } else {
        int status = -2;
        join(pid, &status);
        printf("this is in father process, father process will do something\n");
        int i, x = 0;
        for (i = 0; i < 1000000; i++) {
            x += 2;
        }
        printf("child process %d exit with status %d\n", pid, status);
        printf("father process will exit...\n");
        halt();
    }
}
