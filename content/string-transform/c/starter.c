#include <stdio.h>
#include <string.h>

#define MAX_TEXT 128
#define MAX_WORDS 16
#define MAX_WORD_LENGTH 32

// 아래 선언의 함수 정의와 본문 구현
int split_string(const char *text, char delimiter, char words[][MAX_WORD_LENGTH]);
void join_strings(const char words[][MAX_WORD_LENGTH], int count, char delimiter, char result[]);
void reverse_string(char text[]);
void sort_strings(char words[][MAX_WORD_LENGTH], int count);
