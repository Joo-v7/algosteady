#include <stdio.h>
#include <stdlib.h>

#define MAX_ACTIVITIES 100

typedef struct {
    int id;
    int start;
    int end;
} Activity;

// 종료 시각 → 시작 시각 → 원래 ID 순서 비교
static int compare_activities(const void *left, const void *right)
{
    const Activity *a = left;
    const Activity *b = right;
    if (a->end != b->end) return (a->end > b->end) - (a->end < b->end);
    if (a->start != b->start) return (a->start > b->start) - (a->start < b->start);
    return a->id - b->id;
}

// 종료 시각 기준 선택과 원래 활동 ID 저장
int select_activities(Activity activities[], int n, int selected[]);
