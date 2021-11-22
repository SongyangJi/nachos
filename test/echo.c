#include "stdio.h"
#include "stdlib.h"

int main(int argc, char **argv) {
    printf("echo programme begin\n");
    int i;

    printf("%d arguments\n", argc);

    for (i = 0; i < argc; i++) {
        printf("arg %d: %s\n", i, argv[i]);
    }
    printf("echo programme end\n");
    return 0;
}
