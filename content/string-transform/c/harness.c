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

static void fail(void)
{
    fprintf(stderr, "문자열 결과·널 종료·입력 보존 검사 오류\n");
    exit(2);
}

static int read_words(char words[][MAX_WORD_LENGTH])
{
    char line[16];
    int count = 0;
    if (!read_line(line, sizeof(line)) || sscanf(line, "%d", &count) != 1 || count < 0 || count > MAX_WORDS) fail();
    for (int i = 0; i < count; i++) {
        if (!read_line(words[i], MAX_WORD_LENGTH)) fail();
    }
    return count;
}

int main(void)
{
    char command[16];
    while (read_line(command, sizeof(command))) {
        struct { int before; char words[MAX_WORDS][MAX_WORD_LENGTH]; int after; } tokens;
        struct { int before; char text[MAX_TEXT]; int after; } output;
        tokens.before = tokens.after = output.before = output.after = -987654321;
        memset(tokens.words, 0, sizeof(tokens.words));
        memset(output.text, '#', sizeof(output.text));
        int count = 0;
        if (strncmp(command, "SPLIT ", 6) == 0 && strlen(command) == 7) {
            char text[MAX_TEXT] = {0};
            char original[MAX_TEXT];
            if (!read_line(text, sizeof(text))) fail();
            memcpy(original, text, sizeof(text));
            count = split_string(text, command[6], tokens.words);
            if (memcmp(text, original, sizeof(text)) != 0) fail();
        } else if (strncmp(command, "JOIN ", 5) == 0 && strlen(command) == 6) {
            count = read_words(tokens.words);
            char original[MAX_WORDS][MAX_WORD_LENGTH];
            memcpy(original, tokens.words, sizeof(original));
            join_strings((const char (*)[MAX_WORD_LENGTH])tokens.words, count, command[5], output.text);
            if (memcmp(original, tokens.words, sizeof(original)) != 0) fail();
        } else if (strcmp(command, "REVERSE") == 0) {
            if (!read_line(output.text, sizeof(output.text))) fail();
            reverse_string(output.text);
        } else if (strcmp(command, "SORT") == 0) {
            count = read_words(tokens.words);
            sort_strings(tokens.words, count);
        } else fail();
        if (tokens.before != -987654321 || tokens.after != -987654321 || output.before != -987654321 || output.after != -987654321) fail();
        if (strncmp(command, "SPLIT ", 6) == 0 || strcmp(command, "SORT") == 0) {
            if (count < 0 || count > MAX_WORDS) fail();
            printf("TOKENS %d\n", count);
            for (int i = 0; i < count; i++) {
                if (memchr(tokens.words[i], '\0', MAX_WORD_LENGTH) == NULL) fail();
                printf("[%d] %s\n", i, tokens.words[i]);
            }
        } else {
            if (memchr(output.text, '\0', MAX_TEXT) == NULL) fail();
            printf("TEXT %s\n", output.text);
        }
    }
    return 0;
}
