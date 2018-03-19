#include <functional>
#include "bst_tree.h"

void bst_tree::insert(long long data) {
    auto new_node = new node(data);

    auto *curr = root;
    if (curr == nullptr) {
        root = new_node;
        return;
    }

    while (curr != nullptr && curr->data != data) {
        node *possible_next = nullptr;
        if (curr->data < data) {
            possible_next = curr->right;
            if (possible_next == nullptr && curr->right.compare_exchange_strong(possible_next, new_node)) {
                return;
            }
        } else {
            possible_next = curr->left;
            if (possible_next == nullptr && curr->left.compare_exchange_strong(possible_next, new_node)) {
                return;
            }
        }
        curr = possible_next;
    }

    throw "UnexpectedError";
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
