#include <queue>
#include <algorithm>
#include <unordered_set>
#include <limits>
#include <atomic>
#include <omp.h>
#include <iostream>

#include "iddfs.h"

std::shared_ptr<const state>
dfs_depth(std::shared_ptr<const state> root, unsigned int depth);

std::shared_ptr<const state>
dfs_cost(std::shared_ptr<const state> root, unsigned int max_cost);

void fill_first_data(std::vector<std::shared_ptr<const state>> &first_data, std::shared_ptr<const state> root) {
    auto max_threads = omp_get_max_threads();
    auto successors = root->next_states();
    auto successors_size = successors.size();

    for (int i = 0; i < successors_size; i++) {
        first_data.push_back(successors[i]);
    }

    while (first_data.size() < max_threads) {
        auto ptr = first_data.back();

        if (!ptr->is_goal()) first_data.pop_back();

        auto nexts = ptr->next_states();
        auto nexts_size = nexts.size();
        for (int i = 0; i < nexts_size; i++) {
            first_data.push_back(nexts[i]);
        }
    }
}

// Naimplementujte efektivni algoritmus pro nalezeni nejkratsi (respektive nej-
// levnejsi) cesty v grafu. V teto metode mate ze ukol naimplementovat pametove
// efektivni algoritmus pro prohledavani velkeho stavoveho prostoru. Pocitejte
// s tim, ze Vami navrzeny algoritmus muze bezet na stroji s omezenym mnozstvim
// pameti (radove nizke stovky megabytu). Vhodnym pristupem tak muze byt napr.
// iterative-deepening depth-first search.
//
// Metoda ma za ukol vratit ukazatel na cilovy stav, ktery je dosazitelny pomoci
// nejkratsi/nejlevnejsi cesty.
std::shared_ptr<const state> iddfs(std::shared_ptr<const state> root) {
    if (root->is_goal()) return root;

    std::vector<std::shared_ptr<const state>> first_gen;
    fill_first_data(first_gen, root);

    std::shared_ptr<const state> result = nullptr;
    for (unsigned int i = 2; result == nullptr; i += 1) {
#pragma omp parallel for
        for (int j = 0; j < first_gen.size(); j++) {
            if (result != nullptr) continue;
            result = dfs_depth(first_gen[j], i);
        }
    }
    if (result == nullptr) return nullptr;

    auto max_cost = result->current_cost();
    std::shared_ptr<const state> cost_result = nullptr;
    auto done = true;
    do {
#pragma omp parallel for
        for (int j = 0; j < first_gen.size(); j++) {
            if (!done) continue;

            auto cst = dfs_cost(first_gen[j], max_cost);

            if (cst != nullptr) {
#pragma omp critical
                {
                    if(max_cost > result->current_cost()){
                        result = cst;
                        max_cost = result->current_cost();
                        done = false;
                    }
                }
            }
        }

    } while (!done);

    return result;
}

std::shared_ptr<const state>
dfs_depth(std::shared_ptr<const state> root,
          const unsigned int depth) {
    if (root->is_goal()) {
        return root;
    } else if (depth == 0) {
        return nullptr;
    }

    auto succs = root->next_states();
    auto succs_size = succs.size();
    for (int i = 0; i < succs_size; i++) {
        auto succ = succs[i];
        if (succ->get_identifier() == root->get_predecessor()->get_identifier()) continue;

        auto res = dfs_depth(succ, depth - 1);;
        if (res != nullptr) return res;
    }

    return nullptr;
}

std::shared_ptr<const state>
dfs_cost(std::shared_ptr<const state> root, const unsigned int max_cost) {
    if (root->current_cost() >= max_cost) {
        return nullptr;
    } else if (root->is_goal()) {
        return root;
    }

    auto succs = root->next_states();
    auto succs_size = succs.size();
    for (int i = 0; i < succs_size; i++) {
        auto succ = succs[i];
        if (succ->get_identifier() == root->get_predecessor()->get_identifier()) continue;

        auto res = dfs_cost(succ, max_cost);;
        if (res != nullptr) return res;
    }

    return nullptr;
}