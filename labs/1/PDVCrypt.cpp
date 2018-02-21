//
// Created by karel on 2/10/18.
//

#include "PDVCrypt.h"

PDVCrypt::PDVCrypt(const std::string &alphabet)
        : alphabet_size(alphabet.length()),
          secret((unsigned int *) malloc(27 * 27 * 27 * 27 * 27 * sizeof(unsigned int))) {
    // Initialize tables realizing mapping from alphabet to 0..alphabet_size-1
    int i = 0;
    for (unsigned char c : alphabet) {
        map_table[c] = i;
        unmap_table[i] = c;
        i++;
    }
}

PDVCrypt::PDVCrypt(const std::string &alphabet, std::istream &secretStream) : PDVCrypt(alphabet) {
    loadSecret(secretStream);
}

PDVCrypt::~PDVCrypt() {
    free(secret);
}

// Reads the secret from a stream (typically file)
void PDVCrypt::loadSecret(std::istream &secretStream) {
    const size_t size = 27 * 27 * 27 * 27 * 27 * sizeof(unsigned int);
    secretStream.read((char *) secret, size);
}

// Generates a random secret
void PDVCrypt::generateSecret() {
    const size_t size = 27 * 27 * 27 * 27 * 27;
    for (unsigned int i = 0; i < size; i++) secret[i] = 10000 + rand() % 1000;
}

unsigned char PDVCrypt::MAP(unsigned char c) const {
    return map_table[c];
}

unsigned char PDVCrypt::UNMAP(unsigned char c) const {
    return unmap_table[c];
}

void PDVCrypt::decrypt(std::string &string, const enc_params_t params) const {
    int index = params.start;
    const size_t length = string.length();

    for (unsigned int i = 0; i < params.steps; i++) {
        const unsigned char c1 = MAP(string[(index + length - 2) % length]);
        const unsigned char c2 = MAP(string[(index + length - 1) % length]);
        const unsigned char c3 = MAP(string[(index + length + 0) % length]);
        const unsigned char c4 = MAP(string[(index + length + 1) % length]);
        const unsigned char c5 = MAP(string[(index + length + 2) % length]);

        const unsigned int currentSecret = secret[27 * (27 * (27 * (27 * c1 + c2) + c3) + c4) + c5];

        //+REFERENCE:BEGIN
        string[index] = UNMAP((c3 + params.p1 * currentSecret) % alphabet_size);
        index = (index + params.p2 * currentSecret) % length;
        //+REFERENCE:END
        //+STUDENT // Implement the rules of PDVCrypt. Use params.p1, params.p2, currentSecret
        //+STUDENT // and alphabet_size.
        //+STUDENT string[index] = UNMAP(c3);
        //+STUDENT index = index; 
    }
}
