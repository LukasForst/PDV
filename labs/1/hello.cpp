#include <cstdio>

int main(int argc, char **argv) {
#pragma omp parallel num_threads(64)
    {
        printf("Hello ");
        printf("parallel");
        printf(" world!\n");
    }

    return 0;
}