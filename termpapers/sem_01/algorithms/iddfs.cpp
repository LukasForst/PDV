#include <unordered_set>
#include <queue>
#include "iddfs.h"


bool dfs(const std::shared_ptr<const state> &root,
         std::queue<std::shared_ptr<const state>> &queue,
         std::unordered_set<unsigned long long> &close_list,
         int depth);

std::shared_ptr<const state> result;

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

    std::queue<std::shared_ptr<const state>> queue;
    queue.push(root);

    auto finished = false;
    while (!queue.empty() && !finished) {
        auto s = queue.front();
        queue.pop();

        if (s->is_goal()) return s;

        close_list.insert(s->get_identifier());

        auto next_states = s->next_states();
#pragma omp parallel for
        for (int i = 0; i < next_states.size(); i++) {
            if(finished) continue;
            if (close_list.find(next_states[i]->get_identifier()) != close_list.end()) continue;

            auto res = dfs(next_states[i], queue, close_list, 5);
            if (res) {
                finished = true;
//                return result;
            }
        }
    }

    return result;
}

bool dfs(const std::shared_ptr<const state> &root,
         std::queue<std::shared_ptr<const state>> &queue,
         std::unordered_set<unsigned long long> &close_list,
         int depth) {

    if (root->is_goal()) {
        result = root;
        return true;
    } else if (depth == 0) {
        queue.push(root);
        return false;
    }

    auto next = root->next_states();
    for (int i = 0; i < next.size(); ++i) {
        if (close_list.find(next[i]->get_identifier()) != close_list.end()) continue;
        auto result = dfs(next[i], queue, close_list, depth - 1);
        if (result) {
            return true;
        }

        close_list.insert(next[i]->get_identifier());
    }
    return false;
}