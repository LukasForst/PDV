#include <cstdio>
#include <cstdlib>
#include <chrono>

using namespace std::chrono;

void benchmark(size_t size, size_t jump_size);

constexpr unsigned int ITERS = 100'000'000;
constexpr unsigned int TRIALS = 10;

constexpr size_t KB = 1024;
constexpr size_t MB = 1024 * 1024;

int main(int argc, char **argv) {
    for (unsigned int i = 7; i < 32; i++) {
        benchmark(static_cast<const size_t>(1L << i), 67);
    }

    return 0;
}

void benchmark(const size_t size, const size_t jump_size) {
    auto *memory = (char *) malloc(size * sizeof(char));
    const size_t mask = size - 1; // assuming size=2^n

    std::chrono::steady_clock::time_point begin = std::chrono::steady_clock::now();
    for (unsigned int trial = 0; trial < TRIALS; trial++) {
        size_t index = 0;
        for (unsigned int i = 0; i < ITERS; i++) {
            memory[index] ^= 1;
            index = (index + jump_size) & mask;
        }
    }
    std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();

    unsigned int acc = 0;
    for (size_t i = 0; i < size; i++) acc ^= memory[i];

    if (size < KB) {
        printf(" %4ldB   %8.2fMHz                    %d\n", size,
               (double) (TRIALS * ITERS) / duration_cast<microseconds>(end - begin).count(), acc);
    } else if (size < MB) {
        printf("%4ldKB   %8.2fMHz                    %d\n", size / KB,
               (double) (TRIALS * ITERS) / duration_cast<microseconds>(end - begin).count(), acc);
    } else {
        printf("%4ldMB   %8.2fMHz                    %d\n", size / MB,
               (double) (TRIALS * ITERS) / duration_cast<microseconds>(end - begin).count(), acc);
    }

    free(memory);
}