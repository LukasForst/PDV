#include "integrate.h"

#include <omp.h>
#include <cmath>

using namespace std;

double
integrate_sequential(std::function<double(double)> integrand, double a, double step_size, int step_count) {
    // Promenna kumulujici obsahy jednotlivych obdelniku
    double acc = 0.0;

    for (auto i = 1; i < step_count; i++) {
        auto cx = a + (2 * i + 1) * step_size / 2;
        acc += integrand(cx) * step_size;
    }

    // Celkovy obsah aproximuje hodnotu integralu funkce
    return acc;
}

double
integrate_omp_critical(std::function<double(double)> integrand, double a, double step_size, int step_count) {
    double acc = 0.0;

    // Rozdelte celkovy interval na podintervaly prislusici jednotlivym vlaknum
    // Identifikujte kritickou sekci, kde musi dojit k zajisteni konzistence mezi vlakny
    throw "Not implemented yet";

    return acc;
}


double
integrate_omp_atomic(std::function<double(double)> integrand, double a, double step_size, int step_count) {
    double acc = 0.0;

    //TODO
    // Rozdelte celkovy interval na podintervaly prislusici jednotlivym vlaknum
    // Identifikujte kritickou sekci, kde musi dojit k zajisteni konzistence mezi vlakny


#pragma omp parallel for
    for (auto i = 1; i < step_count; i++) {
        auto cx = a + (2 * i + 1) * step_size / 2;
#pragma omp atomic
        acc += integrand(cx) * step_size;
    }

    return acc;
}

double integrate_omp_reduction(std::function<double(double)> integrand, double a, double step_size, int step_count) {
    double acc = 0.0;

    //TODO
    // Rozdelte celkovy interval na podintervaly prislusici jednotlivym vlaknum
    // Identifikujte kritickou sekci, kde musi dojit k zajisteni konzistence mezi vlakny
#pragma omp parallel for reduction(+:acc)
    for (auto i = 1; i < step_count; i++) {
        auto cx = a + (2 * i + 1) * step_size / 2;
        acc += integrand(cx) * step_size;
    }

    return acc;
}

double integrate_omp_for_static(std::function<double(double)> integrand, double a, double step_size, int step_count) {
    // Promenna kumulujici obsahy jednotlivych obdelniku
    double acc = 0.0;

    //TODO
    //rozsirte integrate_omp_reduction o staticke rozvrhovani
#pragma omp parallel for reduction(+:acc) schedule(static)
    for (auto i = 1; i < step_count; i++) {
        auto cx = a + (2 * i + 1) * step_size / 2;
        acc += integrand(cx) * step_size;
    }

    // Celkovy obsah aproximuje hodnotu integralu funkce
    return acc;
}

double integrate_omp_for_dynamic(std::function<double(double)> integrand, double a, double step_size, int step_count) {
    // Promenna kumulujici obsahy jednotlivych obdelniku
    double acc = 0.0;

    //TODO
    //rozsirte integrate_omp_reduction o dynamicke rozvrhovani
#pragma omp parallel for reduction(+:acc) schedule (dynamic)
    for (auto i = 1; i < step_count; i++) {
        auto cx = a + (2 * i + 1) * step_size / 2;
        acc += integrand(cx) * step_size;
    }

    // Celkovy obsah aproximuje hodnotu integralu funkce
    return acc;
}
