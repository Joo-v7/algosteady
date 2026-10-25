#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void *allocations[256];
static int allocation_count;
static int release_count;

static void fail(void)
{
    fprintf(stderr, "트리 연결 또는 메모리 오류\n");
    exit(2);
}

static int allocation_index(void *node)
{
    for (int i = 0; i < 256; i++) {
        if (allocations[i] == node) return i;
    }
    return -1;
}

static void *allocate_node(size_t size)
{
    for (int i = 0; i < 256; i++) {
        if (allocations[i] == NULL) {
            void *node = malloc(size);
            if (node != NULL) {
                allocations[i] = node;
                allocation_count++;
            }
            return node;
        }
    }
    return NULL;
}

static void release_node(void *node)
{
    if (node == NULL) return;
    int index = allocation_index(node);
    if (index < 0) fail();
    allocations[index] = NULL;
    release_count++;
    free(node);
}

#define malloc allocate_node
#define free release_node
#include "submission.c"
#undef malloc
#undef free

/* 살아 있는 노드와 엄격한 키 순서를 검사한다. */
static void collect_tree(TreeNode *root, TreeNode **nodes, int *count, long long lower, long long upper)
{
    if (root == NULL) return;
    if (allocation_index(root) < 0 || *count == 256) fail();
    for (int i = 0; i < *count; i++) {
        if (nodes[i] == root) fail();
    }
    if (root->key <= lower || root->key >= upper) fail();
    nodes[(*count)++] = root;
    collect_tree(root->left, nodes, count, lower, root->key);
    collect_tree(root->right, nodes, count, root->key, upper);
}

static void print_shape(TreeNode *root)
{
    if (root == NULL) {
        printf("#");
        return;
    }
    printf("%d(", root->key);
    print_shape(root->left);
    printf(",");
    print_shape(root->right);
    printf(")");
}

static void print_values(TreeNode *root)
{
    if (root == NULL) return;
    print_values(root->left);
    printf(" %d", root->key);
    print_values(root->right);
}

static void clear_tree(TreeNode **root)
{
    TreeNode *nodes[256];
    int count = 0;
    collect_tree(*root, nodes, &count, -1000001, 1000001);
    for (int i = 0; i < count; i++) release_node(nodes[i]);
    *root = NULL;
}

int main(void)
{
    TreeNode *roots[2] = {NULL, NULL};
    char command[16];
    int id;
    int key;

    while (scanf("%15s %d", command, &id) == 2) {
        if (id < 0 || id > 1) return 2;
        TreeNode *nodes[256];
        int count = 0;
        collect_tree(roots[id], nodes, &count, -1000001, 1000001);
        if (strcmp(command, "INIT") == 0) {
            clear_tree(&roots[id]);
        } else if (strcmp(command, "NEW") == 0) {
            if (roots[id] != NULL || scanf("%d", &key) != 1) return 2;
            roots[id] = new_node(key);
            if (roots[id] == NULL || allocation_index(roots[id]) < 0) fail();
            if (roots[id]->key != key || roots[id]->left != NULL || roots[id]->right != NULL) fail();
        } else if (strcmp(command, "INSERT") == 0) {
            if (scanf("%d", &key) != 1) return 2;
            roots[id] = insert_node(roots[id], key);
        } else if (strcmp(command, "DELETE") == 0) {
            if (scanf("%d", &key) != 1) return 2;
            roots[id] = delete_node(roots[id], key);
        } else if (strcmp(command, "SEARCH") == 0 || strcmp(command, "MIN") == 0) {
            int minimum = strcmp(command, "MIN") == 0;
            if (minimum && count == 0) return 2;
            if (!minimum && scanf("%d", &key) != 1) return 2;
            TreeNode *expected = NULL;
            TreeNode saved[256];
            for (int i = 0; i < count; i++) {
                saved[i] = *nodes[i];
                if (minimum) {
                    if (expected == NULL || nodes[i]->key < expected->key) expected = nodes[i];
                } else if (nodes[i]->key == key) expected = nodes[i];
            }
            int before_allocations = allocation_count;
            int before_releases = release_count;
            TreeNode *actual = minimum ? min_value_node(roots[id]) : search(roots[id], key);
            if (actual != expected || allocation_count != before_allocations || release_count != before_releases) fail();
            for (int i = 0; i < count; i++) {
                if (allocation_index(nodes[i]) < 0 || nodes[i]->key != saved[i].key
                        || nodes[i]->left != saved[i].left || nodes[i]->right != saved[i].right) fail();
            }
            printf("%s ", command);
            if (actual == NULL) printf("NULL\n");
            else printf("%d\n", actual->key);
        } else if (strcmp(command, "STATE") == 0) {
            printf("STATE");
            print_values(roots[id]);
            printf("\n");
        } else if (strcmp(command, "SHAPE") == 0) {
            printf("SHAPE ");
            print_shape(roots[id]);
            printf("\n");
        } else if (strcmp(command, "ROOT") == 0) {
            if (roots[id] == NULL) printf("ROOT NULL\n");
            else printf("ROOT %d\n", roots[id]->key);
        } else if (strcmp(command, "SIZE") == 0) {
            printf("SIZE %d\n", count);
        } else if (strcmp(command, "STATS") == 0) {
            printf("STATS %d %d\n", allocation_count, release_count);
        } else return 2;

        count = 0;
        collect_tree(roots[0], nodes, &count, -1000001, 1000001);
        collect_tree(roots[1], nodes, &count, -1000001, 1000001);
    }
    clear_tree(&roots[0]);
    clear_tree(&roots[1]);
    for (int i = 0; i < 256; i++) {
        if (allocations[i] != NULL) fail();
    }
    return 0;
}
