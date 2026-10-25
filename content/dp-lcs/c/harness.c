#include <stdlib.h>
#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "입력 문자열·표 계산 범위·배열 경계 검사 오류\n");
    exit(2);
}

static void read_text(char text[])
{
    char line[MAX_LENGTH + 3];
    if (fgets(line, sizeof(line), stdin) == NULL) fail();
    size_t length = strcspn(line, "\r\n");
    if (length > MAX_LENGTH) fail();
    for (size_t i = 0; i < length; i++) {
        if (line[i] < 'A' || line[i] > 'Z') fail();
    }
    memset(text, 0, MAX_LENGTH + 1);
    memcpy(text, line, length);
}

int main(void)
{
    struct { int before; char text[MAX_LENGTH + 1]; int after; } a, b;
    struct { int before; int values[MAX_LENGTH + 1][MAX_LENGTH + 1]; int after; } table;
    a.before = a.after = b.before = b.after = table.before = table.after = -987654321;
    for (int i = 0; i <= MAX_LENGTH; i++) {
        for (int j = 0; j <= MAX_LENGTH; j++) table.values[i][j] = -987654321;
    }
    char command[16];
    while (fgets(command, sizeof(command), stdin) != NULL) {
        if (strcmp(command, "SOLVE\n") != 0) return 2;
        read_text(a.text);
        read_text(b.text);
        int m = (int)strlen(a.text);
        int n = (int)strlen(b.text);

        // 입력과 계산 범위 밖의 기존 표 보관
        char original_a[MAX_LENGTH + 1];
        char original_b[MAX_LENGTH + 1];
        int original_table[MAX_LENGTH + 1][MAX_LENGTH + 1];
        memcpy(original_a, a.text, sizeof(original_a));
        memcpy(original_b, b.text, sizeof(original_b));
        memcpy(original_table, table.values, sizeof(original_table));
        int result = lcs_length(a.text, b.text, table.values);
        if (memcmp(original_a, a.text, sizeof(original_a)) != 0
                || memcmp(original_b, b.text, sizeof(original_b)) != 0) fail();
        if (a.before != -987654321 || a.after != -987654321 || b.before != -987654321 || b.after != -987654321
                || table.before != -987654321 || table.after != -987654321) fail();
        for (int i = 0; i <= MAX_LENGTH; i++) {
            for (int j = 0; j <= MAX_LENGTH; j++) {
                if ((i > m || j > n) && table.values[i][j] != original_table[i][j]) fail();
            }
        }
        printf("LENGTH %d\nTABLE\n", result);
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (j > 0) printf(" ");
                printf("%d", table.values[i][j]);
            }
            printf("\n");
        }
    }
    return 0;
}
