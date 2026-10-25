#include <stdio.h>
#include <string.h>

#define MAX_TEXT 128
#define MAX_PATTERN 64

int brute_force_search(const char *text, const char *pattern)
{
    int n = (int)strlen(text);
    int m = (int)strlen(pattern);
    for (int start = 0; start <= n - m; start++) {
        int matched = 0;
        while (matched < m && text[start + matched] == pattern[matched]) matched++;
        if (matched == m) return start;
    }
    return -1;
}
