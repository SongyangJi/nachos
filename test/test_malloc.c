/**
 * @Author: 吉松阳
 * @Date: 2021/11/23
 * @Description: 
 */

#include "stdio.h"

int main() {
    char s1[1024];
    char s2[1024];
    char s3[1024];
    printf("s1 is at %d\n", s1);
    printf("s2 is at %d\n", s2);
    printf("s3 is at %d\n", s3);


    int size = 100 * 1024 * 1024; // 100 MB
    int MB = 1024 * 1024;
    char *buffer = (char *) malloc(size);
    int i;
    for (i = 0; i < 16; i++) {
        buffer[i * MB] = (char) ((i % 10) + '0');
    }

    printf("buffer is at %d\n", buffer);
    printf("buffer is : ")
    for (i = 0; i < 16; i++) {
        printf("%c", buffer[i * MB]);
    }
    printf("\nfree it\n");
    free(buffer);
    halt();
    return 0;
}