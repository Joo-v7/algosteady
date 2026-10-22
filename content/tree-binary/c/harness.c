#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static void *allocations[128];
static int allocated_nodes;
static int allocation_count;
static int release_count;

/* 생성된 노드를 추적해 연결되지 않은 노드까지 정리한다. */
static void *allocate_node(size_t size)
{
    if (allocated_nodes == 128) return NULL;
    void *node = malloc(size);
    if (node != NULL) {
        allocations[allocated_nodes++] = node;
        allocation_count++;
    }
    return node;
}

static void release_node(void *node)
{
    if (node != NULL) release_count++;
    free(node);
}

#define malloc allocate_node
#define free release_node
#include "submission.c"
#undef malloc
#undef free

/* 등록된 노드의 주소를 확인한 뒤 구조 검사에 사용한다. */
static int node_index(TreeNode **nodes, TreeNode *node)
{
    if (node == NULL) return -1;
    for (int i = 0; i < 128; i++) {
        if (nodes[i] == node) return i;
    }
    fprintf(stderr, "노드 연결 오류\n");
    exit(2);
}

/* 값과 좌우 위치를 출력하며 순환·공유 연결을 검사한다. */
static void print_shape(TreeNode **nodes, TreeNode *node, int *visited)
{
    if (node == NULL) {
        printf("#");
        return;
    }
    int index = node_index(nodes, node);
    if (visited[index]) {
        fprintf(stderr, "트리 연결 오류\n");
        exit(2);
    }
    visited[index] = 1;
    printf("%d(", node->data);
    print_shape(nodes, node->left, visited);
    printf(",");
    print_shape(nodes, node->right, visited);
    printf(")");
}

/* 테스트 호출 코드가 생성한 모든 노드의 메모리를 정리한다. */
static void clear_nodes(TreeNode **nodes)
{
    for (int i = 0; i < allocated_nodes; i++) {
        release_node(allocations[i]);
        allocations[i] = NULL;
    }
    allocated_nodes = 0;
    memset(nodes, 0, 128 * sizeof(TreeNode *));
}

int main(void)
{
    TreeNode *nodes[128] = {0};
    char command[16];
    int id;
    int data;
    int left;
    int right;

    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "CLEAR") == 0) {
            clear_nodes(nodes);
        } else if (strcmp(command, "STATS") == 0) {
            printf("STATS %d %d\n", allocation_count, release_count);
        } else {
            if (scanf("%d", &id) != 1 || id < 0 || id >= 128) return 2;
            if (strcmp(command, "NODE") == 0) {
                if (scanf("%d %d %d", &data, &left, &right) != 3 || nodes[id] != NULL
                        || left < -1 || left >= 128 || right < -1 || right >= 128) return 2;
                if ((left >= 0 && nodes[left] == NULL) || (right >= 0 && nodes[right] == NULL)) return 2;
                nodes[id] = create_node(data, left == -1 ? NULL : nodes[left], right == -1 ? NULL : nodes[right]);
                if (nodes[id] == NULL) return 2;
                for (int i = 0; i < 128; i++) {
                    if (i != id && nodes[i] == nodes[id]) return 2;
                }
            } else if (strcmp(command, "SHAPE") == 0) {
                int visited[128] = {0};
                printf("SHAPE ");
                print_shape(nodes, nodes[id], visited);
                printf("\n");
            } else if (strcmp(command, "LINKS") == 0) {
                if (nodes[id] == NULL) return 2;
                printf("LINKS %d %d\n", node_index(nodes, nodes[id]->left), node_index(nodes, nodes[id]->right));
            } else {
                return 2;
            }
        }
    }

    clear_nodes(nodes);
    return 0;
}
