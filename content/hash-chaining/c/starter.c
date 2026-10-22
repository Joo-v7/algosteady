#include <stdio.h>
#include <stdlib.h>

#define TABLE_SIZE 7

typedef struct {
    int key;
} element;

typedef struct ListNode {
    element item;
    struct ListNode *link;
} ListNode;

int hash_function(int key);
void hash_chain_add(element item, ListNode *ht[]);
ListNode *hash_chain_search(element item, ListNode *ht[]);
void hash_chain_delete(element item, ListNode *ht[]);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
