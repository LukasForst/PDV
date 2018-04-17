#include "iddfs.h"

std::shared_ptr<const state> dfs(std::shared_ptr<const state> &root, std::vector<std::shared_ptr<const state>> &leafs, int depth);

std::shared_ptr<const state> perform_dfs(std::vector<std::shared_ptr<const state>> &roots,
                 std::vector<std::shared_ptr<const state>> &leafs,
                 const int &depth);

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
    std::vector<std::shared_ptr<const state>> leafs;
    auto roots = root->next_states();

    auto dfs_result = perform_dfs(roots, leafs, 2);
    while(dfs_result == nullptr){
        roots.insert(roots.begin(), leafs.begin(), leafs.end());
        leafs.clear();
        dfs_result = perform_dfs(leafs, roots, 2);
    }

    return dfs_result;
}


std::shared_ptr<const state> perform_dfs(std::vector<std::shared_ptr<const state>> &roots,
                 std::vector<std::shared_ptr<const state>> &leafs,
                 const int &depth) {

    std::vector<std::vector<std::shared_ptr<const state>>> results(roots.size());
    auto testing = false;

    std::shared_ptr<const state> result = nullptr;
//#pragma omp parallel for
    for (int i = 0; i < roots.size(); i++) {
        if(testing) continue;
        result = dfs(roots[i], results[i], depth);

        if (result != nullptr) {
//#pragma omp cancel for
            testing = true;
        }
    }
    if(testing){
        return result;
    }

    for (int i = 0; i < results.size(); i++) {
        leafs.insert(leafs.end(), results[i].begin(), results[i].end());
    }
    return nullptr;
}

std::shared_ptr<const state> dfs(std::shared_ptr<const state> &root,
         std::vector<std::shared_ptr<const state>> &leafs,
         const int depth) {
    if(root == nullptr){
        return nullptr;
    } else if(root->is_goal()){
        return root;
    } else if(depth == 0){
        leafs.push_back(root);
        return nullptr;
    }

    auto successors = root->next_states();
    std::shared_ptr<const state> res = nullptr;
    for(int i = 0; i < successors.size(); i++){
        if(i == 0 && depth == 2){
            i = 0;
        }
         res = dfs(successors[i], leafs, depth - 1);
        if(res != nullptr){
            if(res->is_goal()){
                return res;
            }
        }
    }
}