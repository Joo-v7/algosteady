#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct TreeNode {
    element key;
    struct TreeNode *left;
    struct TreeNode *right;
} TreeNode;

TreeNode *search(TreeNode *node, int key);
TreeNode *new_node(int item);
TreeNode *insert_node(TreeNode *node, int key);
TreeNode *min_value_node(TreeNode *node);
TreeNode *delete_node(TreeNode *root, int key);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
