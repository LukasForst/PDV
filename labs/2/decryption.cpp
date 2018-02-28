//
// Created by karel on 12.2.18.
//

#include <vector>
#include <thread>
#include <mutex>
#include <atomic>
#include "decryption.h"

using namespace std;

void decrypt_sequential(const PDVCrypt &crypt, std::vector<std::pair<std::string, enc_params>> &encrypted,
                        unsigned int numThreads) {
    // V sekvencni verzi je pocet vlaken (numThreads) ignorovany
    const unsigned long size = encrypted.size();
    for (unsigned long i = 0; i < size; i++) {
        auto &enc = encrypted[i];
        crypt.decrypt(enc.first, enc.second);
    }
}

void decrypt_openmp(const PDVCrypt &crypt, std::vector<std::pair<std::string, enc_params>> &encrypted,
                    unsigned int numThreads) {
    const unsigned long size = encrypted.size();

#pragma omp parallel for num_threads(numThreads)
    for (unsigned long i = 0; i < size; i++) {
        auto &enc = encrypted[i];
        crypt.decrypt(enc.first, enc.second);
    }
}

void decrypt_threads_1(const PDVCrypt &crypt, std::vector<std::pair<std::string, enc_params>> &encrypted,
                       unsigned int numThreads) {
    const unsigned long size = encrypted.size();
    unsigned long i = 0;

    // process je "vnorena" funkce (lambda funkce) void process(), ktera ma pristup
    // ke vsem promennym
    auto process = [&]() {
        while (i < size) {
            auto &enc = encrypted[i];
            i++;
            crypt.decrypt(enc.first, enc.second);
        }
    };

    // Spustte 'numThreads' vlaken, ktera budou spolecne resit desifrovani retezcu!
    // Vlakna budou desifrovat za pouziti funkce 'process'


    vector<thread> threads;
    for (auto j = 0; j < numThreads; j++) {
        threads.emplace_back(process);
    }

    for (auto j = 0; j < numThreads; j++) {
        threads[j].join();
    }
}

void decrypt_threads_2(const PDVCrypt &crypt, std::vector<std::pair<std::string, enc_params>> &encrypted,
                       unsigned int numThreads) {
    // Opravte problem vznikly v metode 'decrypt_threads_1' pomoci mutexu

    const unsigned long size = encrypted.size();
    unsigned long i = 0;

    mutex m;
    auto process = [&]() {
        while (i < size) {
            m.lock();
            auto &enc = encrypted[i];
            i++;
            m.unlock();
            crypt.decrypt(enc.first, enc.second);
        }
    };

    vector<thread> threads;
    for (auto j = 0; j < numThreads; j++) {
        threads.emplace_back(process);
    }

    for (auto j = 0; j < numThreads; j++) {
        threads[j].join();
    }
}

void decrypt_threads_3(const PDVCrypt &crypt, std::vector<std::pair<std::string, enc_params>> &encrypted,
                       unsigned int numThreads) {
    // Opravte problem vznikly v metode 'decrypt_threads_1' pomoci atomicke promenne

    const unsigned long size = encrypted.size();
    atomic<unsigned long> i(0);

    auto process = [&]() {
        while (i < size) {
            unsigned long currIdx = i++; // backup value to make it safe from other threads
            if (currIdx >= size) break;

            auto &enc = encrypted[currIdx];
            crypt.decrypt(enc.first, enc.second);
        }
    };

    vector<thread> threads;
    for (auto j = 0; j < numThreads; j++) {
        threads.emplace_back(process);
    }

    for (auto &j : threads) {
        j.join();
    }
}

void decrypt_threads_4(const PDVCrypt &crypt, std::vector<std::pair<std::string, enc_params>> &encrypted,
                       unsigned int numThreads) {

    // Doplnte logiku vypoctu disjunktnich rozsahu pro jednotliva vlakna
    // Kod pro spusteni vlaken (a predani rozsahu) je jiz hotovy. Vasim
    // cilem je spocitat hodnoty promennych 'begin' a 'end' pro kazde
    // vlakno.

    const unsigned long size = encrypted.size();
    const unsigned long offset = size / numThreads;
    vector<thread> threads;
    for (unsigned int t = 0; t < numThreads; t++) {

        // Doplnte vypocet rozsahu pro t-te vlakno zde:
        unsigned long begin = offset * t;
        unsigned long end = offset * (t + 1);

        threads.emplace_back([&](unsigned long begin, unsigned long end) {
            for (unsigned long i = begin; i < end; i++) {
                auto &enc = encrypted[i];
                crypt.decrypt(enc.first, enc.second);
            }
        }, begin, end);
    }
    for (unsigned int t = 0; t < numThreads; t++) {
        threads[t].join();
    }
}
