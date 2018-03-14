#ifndef PDV_HW03_BST_H
#define PDV_HW03_BST_H


#include <atomic>

class bst_tree {
public:

    class node {
    public:
        node * left = nullptr;
        node * right = nullptr;

        long long data;

        node(long long data) : data(data) {}
    };

    node * root = nullptr;

    ~bst_tree();
    void insert(long long data);
};


#endif //PDV_HW03_BST_H
