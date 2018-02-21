#include <thread>
#include <vector>

using namespace std;

constexpr size_t NTHREADS = 8;
constexpr size_t STEP = 16;

int data[NTHREADS * STEP];

void inc(volatile int *x) {
    for (unsigned long long i = 0L; i < 1'000'000'000; ++i) {
        if (i & 1) ++*x;
        else *x = (*x) * (*x);
    }
}

int main(int argc, char **argv) {
    std::vector<std::thread> threads;
    std::chrono::steady_clock::time_point begin = std::chrono::steady_clock::now();

    for (unsigned int t = 0; t < NTHREADS; t++) {
        threads.emplace_back(inc, &data[t * STEP]);
    }
    for (unsigned int t = 0; t < NTHREADS; t++) {
        threads[t].join();
    }

    std::chrono::steady_clock::time_point end = std::chrono::steady_clock::now();

    printf("Took: %ldms\n", chrono::duration_cast<chrono::milliseconds>(end - begin).count());

    return 0;
}