#include <stdio.h>
#include <string.h>

#define MAX_TEXT 128
#define MAX_WORDS 16
#define MAX_WORD_LENGTH 32

int split_string(const char *text, char delimiter, char words[][MAX_WORD_LENGTH])
{
    int count = 0;
    int index = 0;
    while (text[index] != '\0') {
        while (text[index] == delimiter) index++;
        if (text[index] == '\0') break;
        int length = 0;
        while (text[index] != '\0' && text[index] != delimiter) {
            words[count][length++] = text[index++];
        }
        words[count++][length] = '\0';
    }
    return count;
}

void join_strings(const char words[][MAX_WORD_LENGTH], int count, char delimiter, char result[])
{
    int length = 0;
    for (int i = 0; i < count; i++) {
        if (i > 0) result[length++] = delimiter;
        for (int j = 0; words[i][j] != '\0'; j++) result[length++] = words[i][j];
    }
    result[length] = '\0';
}

void reverse_string(char text[])
{
    int left = 0;
    int right = (int)strlen(text) - 1;
    while (left < right) {
        char temp = text[left];
        text[left++] = text[right];
        text[right--] = temp;
    }
}

void sort_strings(char words[][MAX_WORD_LENGTH], int count)
{
    for (int i = 0; i < count - 1; i++) {
        for (int j = 0; j < count - 1 - i; j++) {
            if (strcmp(words[j], words[j + 1]) > 0) {
                char temp[MAX_WORD_LENGTH];
                strcpy(temp, words[j]);
                strcpy(words[j], words[j + 1]);
                strcpy(words[j + 1], temp);
            }
        }
    }
}
