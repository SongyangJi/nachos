#include "stdio.h"
#include "stdlib.h"

int main(int argc, char **argv) {

    printf("demo show begin\n");

    char *cs2[3] = {"arg1", "arg2", "arg3"};
    int pid2 = exec("echo.coff", 3, cs2);
    int status2;
    join(pid2, &status2);
    printf("child process %d end.\n", pid2);

    char *cs1[2] = {"test_fileio", "test_text.txt"};
    int pid1 = exec("test_fileio.coff", 2, cs1);
    int status1;
    join(pid1, &status1);
    printf("child process %d end.\n", pid1);


    printf("demo show end\n");
    halt();
}
