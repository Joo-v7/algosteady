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

TrieNode *create_node(void)
{
    TrieNode *node = calloc(1, sizeof(TrieNode));
    if (node == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    return node;
}

void insert_word(TrieNode *root, const char *word)
{
    TrieNode *node = root;
    for (int i = 0; word[i] != '\0'; i++) {
        int index = word[i] - 'a';
        if (node->children[index] == NULL) node->children[index] = create_node();
        node = node->children[index];
    }
    node->is_end = 1;
}

int search_word(const TrieNode *root, const char *word)
{
    const TrieNode *node = root;
    for (int i = 0; word[i] != '\0'; i++) {
        int index = word[i] - 'a';
        if (node->children[index] == NULL) return 0;
        node = node->children[index];
    }
    return node->is_end;
}

int delete_word(TrieNode *root, const char *word)
{
    if (*word == '\0') {
        if (!root->is_end) return 0;
        root->is_end = 0;
        return 1;
    }
    int index = *word - 'a';
    TrieNode *child = root->children[index];
    if (child == NULL) return 0;
    int removed = delete_word(child, word + 1);

    // 다른 단어에서 사용하지 않는 접미 경로만 해제
    if (removed && !child->is_end && !has_children(child)) {
        free(child);
        root->children[index] = NULL;
    }
    return removed;
}

void free_trie(TrieNode *root)
{
    if (root == NULL) return;
    for (int i = 0; i < ALPHABET_SIZE; i++) free_trie(root->children[i]);
    free(root);
}
