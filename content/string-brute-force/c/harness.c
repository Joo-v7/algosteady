#include <stdlib.h>
#include "submission.c"

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
    char command[16];
    while (read_line(command, sizeof(command))) {
        if (strcmp(command, "FIND") != 0) return 2;
        char text[MAX_TEXT] = {0};
        char pattern[MAX_PATTERN] = {0};
        if (!read_line(text, sizeof(text)) || !read_line(pattern, sizeof(pattern))) return 2;
        char text_copy[MAX_TEXT];
        char pattern_copy[MAX_PATTERN];
        memcpy(text_copy, text, sizeof(text));
        memcpy(pattern_copy, pattern, sizeof(pattern));
        int result = brute_force_search(text, pattern);
        if (memcmp(text_copy, text, sizeof(text)) != 0 || memcmp(pattern_copy, pattern, sizeof(pattern)) != 0) return 2;
        printf("INDEX %d\n", result);
    }
    return 0;
}
