#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct TreeNode {
    element key;
    struct TreeNode *left;
    struct TreeNode *right;
} TreeNode;

TreeNode *search(TreeNode *node, int key)
{
    if (node == NULL) return NULL;
    if (key == node->key) return node;
    if (key < node->key) return search(node->left, key);
    return search(node->right, key);
}

TreeNode *new_node(int item)
{
    TreeNode *node = malloc(sizeof(TreeNode));
    if (node == NULL) {
        fprintf(stderr, "메모리 할당 오류\n");
        exit(1);
    }
    node->key = item;
    node->left = NULL;
    node->right = NULL;
    return node;
}

TreeNode *insert_node(TreeNode *node, int key)
{
    if (node == NULL) return new_node(key);
    if (key < node->key) node->left = insert_node(node->left, key);
    else if (key > node->key) node->right = insert_node(node->right, key);
    return node;
}

TreeNode *min_value_node(TreeNode *node)
{
    TreeNode *current = node;
    while (current->left != NULL) current = current->left;
    return current;
}

TreeNode *delete_node(TreeNode *root, int key)
{
    if (root == NULL) return root;
    if (key < root->key) root->left = delete_node(root->left, key);
    else if (key > root->key) root->right = delete_node(root->right, key);
    else {
        if (root->left == NULL) {
            TreeNode *next = root->right;
            free(root);
            return next;
        }
        if (root->right == NULL) {
            TreeNode *next = root->left;
            free(root);
            return next;
        }
        TreeNode *next = min_value_node(root->right);
        root->key = next->key;
        root->right = delete_node(root->right, next->key);
    }
    return root;
}
