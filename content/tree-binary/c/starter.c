#include <stdio.h>
#include <stdlib.h>

typedef struct TreeNode {
    int data;
    struct TreeNode *left;
    struct TreeNode *right;
} TreeNode;

TreeNode *create_node(int data, TreeNode *left, TreeNode *right);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
