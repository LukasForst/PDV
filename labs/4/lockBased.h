#ifndef LOCKBASED_H
#define LOCKBASED_H

#include <vector>
#include <iostream>
#include <atomic>

class spin_mutex {
private:
    std::atomic_flag flag;

public:
    spin_mutex() : flag(ATOMIC_FLAG_INIT) {}

    void lock() {
        while (flag.test_and_set(std::memory_order_acq_rel));
    }

    void unlock() {
        flag.clear();
    }

};

class Concurrent {
public:
    class Node {
    public:
        long long value;
        Node *next = nullptr;
        spin_mutex m;

        Node(long long value) : value(value) {}
    };

    Node *head = new Node(-999999999999L);
    spin_mutex head_mutex;

    void add(long long value) {

        Node *node = new Node(value);

        // pokud je list prazdny, vytvorte koren
        // jinak najdete misto kam hodnotu vlozit, a tam ji bezpecne vlozte
        head_mutex.lock();

        if(head == nullptr) {
            head = node;
            head_mutex.unlock();
        }
        else {
            Node * current = head;
            head_mutex.unlock();

            while(current->next != nullptr && current->next->value < value) {
                current = current->next;
            }

            current->m.lock();

            if(current->value > value || (current->next != nullptr && current->next->value < value)){
                current->m.unlock();
                add(value);
                return;
            }
            node->next = current->next;
            current->next = node;
            current->m.unlock();
        }

    }

    void remove(long long value) {
    }
};

#endif