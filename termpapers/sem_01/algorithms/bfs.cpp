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
    if(root->is_goal()) return root;
    std::shared_ptr<const state> result;
    std::atomic<unsigned int> max_cost = {std::numeric_limits<unsigned int>::max()};

    std::unordered_set<unsigned long long> close_list;

    std::vector<std::shared_ptr<const state>> open_list;
    close_list.insert(root->get_identifier());

    auto nexts = root->next_states();
    for(const auto &i : nexts){
        if(i->is_goal()) {
            auto cost = i->current_cost();
            if(cost < max_cost){
                max_cost = i->current_cost();
                result = i;
            }
        }

        open_list.push_back(i);
        close_list.insert(i->get_identifier());
    }

    while (!open_list.empty()) {
        auto size = open_list.size();

#pragma omp parallel for
        for (int i = 0; i < size; i++) {
            std::vector<std::shared_ptr<const state>> op;
            auto current = open_list[i];
            auto next_states = current->next_states();

            for (int j = 0; j < next_states.size(); j++) {
                auto next = next_states[j];
                auto cost = next->current_cost();

                if (next->is_goal()) {
                    result = next;
                    max_cost = cost;
                    continue;
                } else if (cost >= max_cost || close_list.find(next_states[j]->get_identifier()) != close_list.end()) {
                    continue;
                }

                op.push_back(next);
            }

#pragma omp critical
            {
                for (const auto &j : op) {
                    open_list.push_back(j);
                }
            }
        }

        open_list.erase(open_list.begin(), open_list.begin() + size);
        std::for_each(open_list.begin(), open_list.end(),
                      [&](std::shared_ptr<const state> x) { close_list.insert(x->get_identifier()); });
    }
    return result;
}