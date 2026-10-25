#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_LIVE 2048
#define MAX_STORED_WORDS 64

static void *live[MAX_LIVE];
static int live_count;

static void fail(void)
{
    fprintf(stderr, "트라이 구조·단어 끝·공유 경로·메모리 검사 오류\n");
    exit(2);
}

static int is_live(const void *pointer)
{
    for (int i = 0; i < MAX_LIVE; i++) {
        if (live[i] == pointer && pointer != NULL) return 1;
    }
    return 0;
}

static void *track(void *pointer)
{
    if (pointer == NULL) return NULL;
    for (int i = 0; i < MAX_LIVE; i++) {
        if (live[i] == NULL) {
            live[i] = pointer;
            live_count++;
            return pointer;
        }
    }
    fail();
    return NULL;
}

static void *tracked_malloc(size_t size)
{
    return track(malloc(size));
}

static void *tracked_calloc(size_t count, size_t size)
{
    return track(calloc(count, size));
}

static void tracked_free(void *pointer)
{
    if (pointer == NULL) return;
    for (int i = 0; i < MAX_LIVE; i++) {
        if (live[i] == pointer) {
            live[i] = NULL;
            live_count--;
            free(pointer);
            return;
        }
    }
    fail();
}

#define malloc tracked_malloc
#define calloc tracked_calloc
#define free tracked_free
#include "submission.c"
#undef malloc
#undef calloc
#undef free

static char stored[MAX_STORED_WORDS][MAX_WORD_LENGTH];
static int stored_count;

static int word_index(const char *word)
{
    for (int i = 0; i < stored_count; i++) {
        if (strcmp(stored[i], word) == 0) return i;
    }
    return -1;
}

static int check_node(const TrieNode *node, char prefix[], int depth)
{
    if (!is_live(node) || depth >= MAX_WORD_LENGTH) fail();
    prefix[depth] = '\0';
    if (node->is_end != (word_index(prefix) >= 0)) fail();
    int count = 1;
    for (int letter = 0; letter < ALPHABET_SIZE; letter++) {
        int needed = 0;
        for (int i = 0; i < stored_count; i++) {
            if ((int)strlen(stored[i]) > depth && strncmp(stored[i], prefix, (size_t)depth) == 0
                    && stored[i][depth] == 'a' + letter) needed = 1;
        }
        if ((node->children[letter] != NULL) != needed) fail();
        if (needed) {
            prefix[depth] = (char)('a' + letter);
            count += check_node(node->children[letter], prefix, depth + 1);
            prefix[depth] = '\0';
        }
    }
    return count;
}

static int read_line(char *text, size_t capacity)
{
    if (fgets(text, (int)capacity, stdin) == NULL) return 0;
    size_t length = strlen(text);
    if (length > 0 && text[length - 1] == '\n') text[length - 1] = '\0';
    else if (!feof(stdin)) {
        int next = fgetc(stdin);
        if (next != '\n' && next != EOF) return 0;
    }
    return 1;
}

int main(void)
{
    (void)tracked_malloc;
    (void)tracked_calloc;
    TrieNode *root = create_node();
    char prefix[MAX_WORD_LENGTH];
    if (check_node(root, prefix, 0) != live_count) fail();
    char command[16];
    while (read_line(command, sizeof(command))) {
        if (strcmp(command, "CLEAR") == 0) {
            free_trie(root);
            if (live_count != 0) fail();
            stored_count = 0;
            root = create_node();
            printf("CLEARED\n");
        } else {
            char word[MAX_WORD_LENGTH] = {0};
            char original[MAX_WORD_LENGTH];
            if (!read_line(word, sizeof(word))) fail();
            for (int i = 0; word[i] != '\0'; i++) {
                if (word[i] < 'a' || word[i] > 'z') fail();
            }
            memcpy(original, word, sizeof(word));
            int index = word_index(word);
            if (strcmp(command, "INSERT") == 0) {
                insert_word(root, word);
                if (index < 0) {
                    if (stored_count == MAX_STORED_WORDS) fail();
                    strcpy(stored[stored_count++], word);
                }
            } else if (strcmp(command, "SEARCH") == 0) {
                int found = search_word(root, word);
                if (found != (index >= 0)) fail();
                printf("FOUND %d\n", found);
            } else if (strcmp(command, "DELETE") == 0) {
                int removed = delete_word(root, word);
                if (removed != (index >= 0)) fail();
                if (index >= 0) {
                    stored_count--;
                    if (index != stored_count) strcpy(stored[index], stored[stored_count]);
                }
                printf("DELETED %d\n", removed);
            } else fail();
            if (memcmp(word, original, sizeof(word)) != 0) fail();
        }
        if (check_node(root, prefix, 0) != live_count) fail();
    }
    free_trie(root);
    free_trie(NULL);
    if (live_count != 0) fail();
    return 0;
}
