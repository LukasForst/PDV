#include "sort.h"
#include <omp.h>

struct sort_data {
    const MappingFunction &mapping_function;
    unsigned long alphabet_size;
    unsigned long string_lengths;
};

void recursive_radix_sort(std::vector<std::string *> &vector_to_sort, const sort_data &data,
                          const unsigned int letter_index) {
    if (letter_index == data.string_lengths)
        return;

    std::vector<std::string *> buckets[data.alphabet_size];
    std::for_each(vector_to_sort.begin(), vector_to_sort.end(),
                  [&](std::string *x) { buckets[data.mapping_function((*x).at(letter_index))].push_back(x); });

#pragma omp parallel for
    for (auto i = 0; i < data.alphabet_size; i++) {
        recursive_radix_sort(buckets[i], data, letter_index + 1);
    }

    auto offset = 0;
    for (auto i = 0; i < data.alphabet_size; i++) {
        auto size = buckets[i].size();
        for (auto j = 0; j < size; j++) {
            vector_to_sort[offset++] = buckets[i][j];
        }
    }
}

void radix_par(std::vector<std::string *> &vector_to_sort, const MappingFunction &mapping_function,
               unsigned long alphabet_size, unsigned long string_lengths) {
    sort_data data{
            mapping_function,
            alphabet_size,
            string_lengths
    };
    recursive_radix_sort(vector_to_sort, data, 0);
}