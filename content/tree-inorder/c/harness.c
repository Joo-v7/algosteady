#include <stdlib.h>
#include <string.h>

#include "submission.c"

int main(void)
{
    int repeat;
    int count;
    int scanned;
    while ((scanned = scanf("%d %d", &repeat, &count)) == 2) {
        if (repeat < 1 || repeat > 3 || count < 0 || count > 63) return 2;
        TreeNode nodes[63] = {0};
        TreeNode original[63];
        int present[63] = {0};
        char token[32];
        for (int i = 0; i < count; i++) {
            if (scanf("%31s", token) != 1) return 2;
            if (strcmp(token, "#") == 0) continue;
            char *end;
            long value = strtol(token, &end, 10);
            if (*end != '\0' || value < -1000000 || value > 1000000) return 2;
            if (i > 0 && !present[(i - 1) / 2]) return 2;
            nodes[i].data = (int)value;
            present[i] = 1;
        }
        for (int i = 0; i < count; i++) {
            if (!present[i]) continue;
            if (i * 2 + 1 < count && present[i * 2 + 1]) nodes[i].left = &nodes[i * 2 + 1];
            if (i * 2 + 2 < count && present[i * 2 + 2]) nodes[i].right = &nodes[i * 2 + 2];
        }
        memcpy(original, nodes, sizeof(nodes));

        for (int run = 0; run < repeat; run++) {
            inorder(count > 0 && present[0] ? &nodes[0] : NULL);
            printf("\n");
            for (int i = 0; i < count; i++) {
                if (nodes[i].data != original[i].data || nodes[i].left != original[i].left || nodes[i].right != original[i].right) {
                    fprintf(stderr, "순회 중 트리 변경\n");
                    return 2;
                }
            }
        }
    }
    return scanned == EOF ? 0 : 2;
}
