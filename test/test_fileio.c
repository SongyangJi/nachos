#include "stdio.h"
#include "stdlib.h"

#define MAXSIZE 1024

int main(int argc, char **argv) {
    if (argc != 2) {
        printf("must input one file name\n");
        return -1;
    }
    char *name = argv[1];
    printf("open file name : %s\n", name);
    int fd = open(name);
    if (fd < 0) {
        printf("open file failed!\n");
        return -1;
    }
    printf("file descriptor is %d\n", fd);
    printf("now file content is ...\n");
    char buffer[MAXSIZE];
    int n;
    while ((n = read(fd, buffer, MAXSIZE)) > 0) {
        write(stdout, buffer, n);
    }
    printf("\n");

    printf("please input 3 lines words, they will be written into this file\n");

    int i;
    for (i = 0; i < 3; ++i) {
        memset(buffer, 0, MAXSIZE);
        n = read(stdin, buffer, MAXSIZE);
        printf("your inout text is %s\n", buffer);
        write(fd, buffer, n);
    }
    close(fd);
    printf("close this file, demo end\n");
    exit(0);
}
