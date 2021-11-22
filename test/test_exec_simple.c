#include "stdio.h"
#include "stdlib.h"

int main(int argc, char **argv) {
    printf("demo show exec\n");
    char *cs2[3] = {"arg1", "agg2", "arg3"};
    exec("echo.coff", 3, cs2);
    exit(0);
}
