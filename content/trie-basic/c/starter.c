#include <stdio.h>
#include <stdlib.h>

#define ALPHABET_SIZE 26
#define MAX_WORD_LENGTH 32

typedef struct TrieNode {
    int is_end;
    struct TrieNode *children[ALPHABET_SIZE];
} TrieNode;

static int has_children(const TrieNode *node)
{
    for (int i = 0; i < ALPHABET_SIZE; i++) {
        if (node->children[i] != NULL) return 1;
    }
    return 0;
}

// 아래 선언의 함수 정의와 본문 구현
TrieNode *create_node(void);
void insert_word(TrieNode *root, const char *word);
int search_word(const TrieNode *root, const char *word);
int delete_word(TrieNode *root, const char *word);
void free_trie(TrieNode *root);
