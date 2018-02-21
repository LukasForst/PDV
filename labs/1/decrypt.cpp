#include <iostream>
#include <fstream>
#include <chrono>

#include "PDVCrypt.h"

using namespace std;

int main(int argc, char **argv) {
    ifstream crypttable("secret.dat");
    PDVCrypt crypt(" ABCDEFGHIJKLMNOPQRSTUVWXYZ", crypttable);

    vector<pair<string, enc_params_t>> encrypted;
    ifstream crypts("encrypted.txt");
    enc_params params;
    while (crypts >> params.p1 >> params.p2 >> params.start >> params.steps) {
        string s;
        getline(crypts, s);
        getline(crypts, s);
        encrypted.emplace_back(s.substr(1, s.length() - 2), params);
    }

    const size_t size = encrypted.size();
    chrono::steady_clock::time_point begin = chrono::steady_clock::now();
    for (unsigned int i = 0; i < size; i++) {
        auto &enc = encrypted[i];
        crypt.decrypt(enc.first, enc.second);
    }
    chrono::steady_clock::time_point end = chrono::steady_clock::now();

    for (auto &&enc : encrypted) cout << enc.first << endl;
    cout << "Elapsed: " << chrono::duration_cast<std::chrono::milliseconds>(end - begin).count() << "ms" << endl;

    return 0;
}