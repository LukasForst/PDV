#include <cstdio>
#include <chrono>
#include <cmath>
#include <omp.h>

#include "timing.h"
#include "cpu_info.h"

void magic_operation(double *array) {
#pragma omp parallel for
    for (unsigned int i = 0; i < 1'000'000; i++) {
        for (unsigned int k = 0; k < 500; k++) {
            array[i] = exp(log(array[i]));
        }
    }
}

int main(int argc, char **argv) {
    auto *array = new double[1'000'000];

    omp_set_num_threads(get_num_cores());

    START_TIMING(loop)
    magic_operation(array);
    STOP_TIMING(loop, "magic_operation")

    delete[] array;

    return 0;
}