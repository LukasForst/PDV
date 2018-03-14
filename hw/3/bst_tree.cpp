#include <functional>
#include "bst_tree.h"

void bst_tree::insert(long long data) {
}

bst_tree::~bst_tree() {
    std::function<void(node*)> cleanup = [&](node * n) {
        if(n != nullptr) {
            cleanup(n->left);
            delete n;
            cleanup(n->right);
        }
    };
    cleanup(root);
}
