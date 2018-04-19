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
    std::unordered_set<unsigned long long> close_list;

    std::vector<std::shared_ptr<const state>> open_list;
    open_list.push_back(root);
    std::shared_ptr<const state> result;
    std::atomic<unsigned int> max_cost = {std::numeric_limits<unsigned int>::max()};

    while (!open_list.empty()) {
        auto curr_size = open_list.size();

//#pragma omp parallel for
        for (int i = 0; i < curr_size; i++) {
            std::vector<std::shared_ptr<const state>> op;
            auto s = open_list[i];
            auto cost = s->current_cost();
            if (cost >= max_cost) {
                continue;
            } else if (s->is_goal()) {
#pragma omp critical
                {
                    max_cost.exchange(cost);
                    result = s;
                }
                continue;
            }

            auto next_states = s->next_states();
            for (int j = 0; j < next_states.size(); j++) {
                if (next_states[j]->current_cost() >= max_cost) continue;
                if (close_list.find(next_states[j]->get_identifier()) != close_list.end()) continue;

//                op.push_back(next_states[j]);
                std::shared_ptr<const state> res = dfs(next_states[i], op, close_list, 3);
                if(!res){
                    op.push_back(res);
                }
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