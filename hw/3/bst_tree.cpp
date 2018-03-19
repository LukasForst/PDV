#include <functional>
#include "bst_tree.h"

void bst_tree::insert(long long data) {
    auto new_node = new node(data);

    node *curr = root;
    if (curr == nullptr) {
        root = new_node;
        return;
    }

    while (true) {
        node *next = nullptr;
        if (curr->data < data) {
            next = curr->right;
            if (next == nullptr && curr->right.compare_exchange_strong(next, new_node)) {
                break;
            }
            curr = curr->right;

        } else {
            next = curr->left;
            if (next == nullptr && curr->left.compare_exchange_strong(next, new_node)) {
                break;
            }
            curr = curr->left;
        }
    }

}

bst_tree::~bst_tree() {
    // Rekurzivni funkce pro pruchod stromu a dealokaci pameti prirazene jednotlivym
    // uzlum
    std::function<void(node *)> cleanup = [&](node *n) {
        if (n != nullptr) {
            cleanup(n->left);
            cleanup(n->right);

            delete n;
        }
    };
    cleanup(root);
}
