#include <queue>
#include <algorithm>
#include <unordered_set>
#include <limits>
#include <atomic>
#include <omp.h>
#include <iostream>

#include "iddfs.h"


std::shared_ptr<const state> dfs(std::shared_ptr<const state> root_upp,
                                 std::vector<std::shared_ptr<const state>> &open_list,
                                 std::unordered_set<unsigned long long> &close_list,
                                 int depth);

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
//    if(root->is_goal()) return root;
    return root;
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
            continue;
        }

        open_list.push_back(i);
        close_list.insert(i->get_identifier());
    }

    auto cache_hit = 0;
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
                    if(max_cost > cost){
                        result = next;
                        max_cost = cost;
                    }
                    continue;
                } else if(next->get_identifier() == current->get_predecessor()->get_identifier()){
                    continue;
                } else if (cost >= max_cost || close_list.find(next_states[j]->get_identifier()) != close_list.end()) {
#pragma omp atomic
                    cache_hit++;
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

std::shared_ptr<const state> dfs(std::shared_ptr<const state> root_upp,
                                 std::vector<std::shared_ptr<const state>> &open_list,
                                 std::unordered_set<unsigned long long> &close_list,
                                 int depth) {
    std::shared_ptr<const state> root = root_upp;

    if (root->is_goal()) {
        return root;
    } else if (depth == 0) {
        open_list.push_back(root);
        return nullptr;
    }

    auto next = root->next_states();
    for (int i = 0; i < next.size(); ++i) {
        if (close_list.find(next[i]->get_identifier()) != close_list.end()) continue;
        auto result = dfs(next[i], open_list, close_list, depth - 1);
        if (!result) {
            return result;
        }
    }
    return nullptr;
}