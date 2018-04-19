#include <queue>
#include <algorithm>
#include <unordered_set>
#include "bfs.h"
#include <limits>
#include <atomic>
#include <omp.h>
#include <iostream>

// Naimplementujte efektivni algoritmus pro nalezeni nejkratsi cesty v grafu.
// V teto metode nemusite prilis optimalizovat pametove naroky, a vhodnym algo-
// ritmem tak muze byt napriklad pouziti prohledavani do sirky (breadth-first
// search.
//
// Metoda ma za ukol vratit ukazatel na cilovy stav, ktery je dosazitelny pomoci
// nejkratsi cesty.
std::shared_ptr<const state> bfs(std::shared_ptr<const state> root) {
    std::unordered_set<unsigned long long> close_list;

    std::vector<std::shared_ptr<const state>> open_list;
    open_list.push_back(root);
    std::shared_ptr<const state> result;
    std::atomic<unsigned int> max_cost = {std::numeric_limits<unsigned int>::max()};

    while (!open_list.empty()) {
        std::vector<std::shared_ptr<const state>> data = open_list;
        open_list.clear();
        std::for_each(data.begin(), data.end(),
                      [&](std::shared_ptr<const state> x) { close_list.insert(x->get_identifier()); });

        auto data_size = data.size();
#pragma omp parallel for
        for (int i = 0; i < data_size; i++) {

            auto s = data[i];
            auto cost = s->current_cost();
            if (cost >= max_cost) continue;
            if (s->is_goal()) {
                max_cost.exchange(cost);
                result = s;
            }

            auto next_states = s->next_states();
            for (int j = 0; j < next_states.size(); j++) {
                if (next_states[j]->current_cost() >= max_cost) continue;
                if (close_list.find(next_states[j]->get_identifier()) != close_list.end()) {
                    continue;
                }
#pragma omp critical
                open_list.push_back(next_states[j]);
            }
        }
    }
    return result;
}