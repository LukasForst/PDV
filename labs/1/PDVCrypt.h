//
// Created by karel on 2/10/18.
//

#ifndef PDVCRYPT_PDVCRYPT_H
#define PDVCRYPT_PDVCRYPT_H

#include <cstdlib>
#include <fstream>
#include <vector>

typedef struct enc_params {
    unsigned long long p1;
    unsigned long long p2;
    unsigned int start;
    unsigned int steps;

    enc_params(unsigned long long p1, unsigned long long p2, unsigned int start, unsigned int steps)
            : p1(p1), p2(p2), start(start), steps(steps) {};

    enc_params() : enc_params(0, 0, 0, 0) {}

} enc_params_t;

class PDVCrypt {

private:
    const size_t alphabet_size;

    unsigned int *secret = nullptr;
    unsigned char map_table[255];
    unsigned char unmap_table[255];

    unsigned char MAP(unsigned char c) const;

    unsigned char UNMAP(unsigned char c) const;

public:
    PDVCrypt(const std::string &alphabet);

    PDVCrypt(const std::string &alphabet, std::istream &secretStream);

    ~PDVCrypt();

    void loadSecret(std::istream &secretStream);

    void generateSecret();

    void decrypt(std::string &string, const enc_params_t params) const;

    unsigned int getSecret(unsigned char c1, unsigned char c2, unsigned char c3, unsigned char c4, unsigned char c5) {
        return secret[27 * (27 * (27 * (27 * c1 + c2) + c3) + c4) + c5];
    }
};


#endif //PDVCRYPT_PDVCRYPT_H
