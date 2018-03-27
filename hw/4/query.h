#ifndef DATABASEQUERIES_QUERY_H
#define DATABASEQUERIES_QUERY_H

#include <vector>
#include <functional>
#include <atomic>

template<typename row_t>
using predicate_t = std::function<bool(const row_t &)>;


template<typename row_t>
bool is_satisfied_for_all(std::vector<predicate_t<row_t>> predicates, std::vector<row_t> data_table);

template<typename row_t>
bool is_satisfied_for_any(std::vector<predicate_t<row_t>> predicates, std::vector<row_t> data_table);


template<typename row_t>
bool is_satisfied_for_all(std::vector<predicate_t<row_t>> predicates, std::vector<row_t> data_table) {
    // Doimplementujte telo funkce, ktera rozhodne, zda pro VSECHNY dilci dotazy (obsazene ve
    // vektoru 'predicates') existuje alespon jeden zaznam v tabulce (reprezentovane vektorem
    // 'data_table'), pro ktery je dany predikat splneny.

    // Pro inspiraci si prostudujte kod, kterym muzete zjistit, zda prvni dilci dotaz plati,
    // tj., zda existuje alespon jeden radek v tabulce, pro ktery predikat reprezentovany
    // funkci predicates[i] plati:
    volatile bool result = true;
    auto preds = predicates.size();
    auto row_count = data_table.size();

#pragma omp parallel for shared(result)
    for (int i = 0; i < preds; i++) {
        if (!result) continue;

        auto &predicate = predicates[i];
        auto pred_is_satisfied = false;

        for (unsigned int j = 0; j < row_count; j++) {
            auto &row = data_table[j];                // j-ty radek tabulky ...
            bool is_satisfied = predicate(row);  // ... splnuje prvni predikat, pokud funkce first_predicate

            if (is_satisfied) {
                pred_is_satisfied = true;
                break;
            }
        }

        if (!pred_is_satisfied) {
            result = false;
        }

    }

    return result;
}

template<typename row_t>
bool is_satisfied_for_any(std::vector<predicate_t<row_t>> predicates, std::vector<row_t> data_table) {
    // Doimplementujte telo funkce, ktera rozhodne, zda je ALESPON JEDEN dilci dotaz pravdivy.
    // To znamena, ze mate zjistit, zda existuje alespon jeden predikat 'p' a jeden zaznam
    // v tabulce 'r' takovy, ze p(r) vraci true.

    // Zamyslete se nad tim, pro ktery druh dotazu je vhodny jaky druh paralelizace. Vas
    // kod optimalizujte na situaci, kdy si myslite, ze navratova hodnota funkce bude true.
    // Je pro Vas dulezitejsi rychle najit splnujici radek pro jeden vybrany predikat, nebo
    // je dulezitejsi zkouset najit takovy radek pro vice predikatu paralelne?

    volatile bool result = false;
    auto preds = predicates.size();
    auto row_count = data_table.size();

#pragma omp parallel for shared(result)
    for (int i = 0; i < row_count; i++) {
        if (result) continue;

        auto &row = data_table[i];
        auto pred_is_satisfied = false;
        for (unsigned int j = 0; j < preds; j++) {
            auto &predicate = predicates[j];

            bool is_satisfied = predicate(row);

            if (is_satisfied) {
                pred_is_satisfied = true;
                break;
            }
        }

        if (pred_is_satisfied) {
            result = true;
        }
    }

    return result;
}


#endif //DATABASEQUERIES_QUERY_H
