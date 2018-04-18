#include <queue>
#include <algorithm>
#include <unordered_set>
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

    std::queue<std::shared_ptr<const state>> queue;
    queue.push(root);

    while (!queue.empty()) {
        auto s = queue.front();
        queue.pop();
        if (s->is_goal()) return s;
        close_list.insert(s->get_identifier());

        auto next_states = s->next_states();
        for (int i = 0; i < next_states.size(); i++) {
            bool contains = close_list.find(next_states[i]->get_identifier()) != close_list.end();
            if (contains) continue;

            queue.push(next_states[i]);
        }
    }

    return root;
}