#ifndef LOCKFREE_H
#define LOCKFREE_H

#include <vector>
#include <iostream>
#include <atomic>

class Lockfree {
public:
    class Node {
    public:
        std::atomic<unsigned long long> value;
        std::atomic<Node*> next { nullptr };

        Node(unsigned long long value) : value(value) {}
    };

    static constexpr unsigned long long mask = 1L << 63;
    Node * head = new Node(42L | mask);

    void add(unsigned long long value) {
        Node * node = new Node(value);

        Node * current = head;
        Node * next = current->next;
        while(true) {
            // doplne kontrolu jestli na aktualni pozici pridat vkladanou hodnotu
            // nebo se posunte seznamem dale

            if(next != nullptr && next->value < value) {
                current = next;
                next = (Node*) next->next;
                continue;
            }
            node->next = next;
            if(current->next.compare_exchange_strong(next, node)){
                return;
            }
        }
    }

    bool remove(unsigned long long value) {
        return false;
    }
};

#endif