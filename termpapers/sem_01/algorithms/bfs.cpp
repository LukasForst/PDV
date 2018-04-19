#include <queue>
#include <algorithm>
#include <unordered_set>
#include <limits>
#include <atomic>
#include <omp.h>

#include "bfs.h"

// Naimplementujte efektivni algoritmus pro nalezeni nejkratsi cesty v grafu.
// V teto metode nemusite prilis optimalizovat pametove naroky, a vhodnym algo-
// ritmem tak muze byt napriklad pouziti prohledavani do sirky (breadth-first
// search.
//
// Metoda ma za ukol vratit ukazatel na cilovy stav, ktery je dosazitelny pomoci
// nejkratsi cesty.
std::shared_ptr<const state> bfs(std::shared_ptr<const state> root) {
    std::unordered_set<unsigned long long> close_list;

    close_list.insert(root->get_identifier());
    std::vector<std::shared_ptr<const state>> succs = root->next_states();
    std::vector<std::shared_ptr<const state>> open_list(succs.size());

    for (int i = 0; i < succs.size(); i++) {
        open_list.push_back(succs[i]);
        close_list.insert(succs[i]->get_identifier());
    }

    std::shared_ptr<const state> result;
    std::atomic<unsigned int> max_cost = {std::numeric_limits<unsigned int>::max()};


    while (!open_list.empty()) {
        auto curr_size = open_list.size();

#pragma omp parallel for
        for (int i = 0; i < curr_size; i++) {
            std::vector<std::shared_ptr<const state>> op;
            std::shared_ptr<const state> current = open_list[i];
            auto next_states = current->next_states();
            for (int j = 0; j < next_states.size(); j++) {
                std::shared_ptr<const state> next = next_states[i];
                auto cost = next->current_cost();

                if (cost >= max_cost) {
                    continue;
                }

                if (next->is_goal()) {
                    max_cost.exchange(cost);
                    result = next;
                    continue;
                }

                if (close_list.find(next->get_identifier()) != close_list.end()) continue;
                op.push_back(next);
            }
#pragma omp critical
            open_list.insert(open_list.end(), op.begin(), op.end());
        }

        open_list.erase(open_list.begin(), open_list.begin() + curr_size);
        std::for_each(open_list.begin(), open_list.end(),
                      [&](std::shared_ptr<const state> x) { close_list.insert(x->get_identifier()); });
    }
    return result;
}