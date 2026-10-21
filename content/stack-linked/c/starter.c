#include <stdio.h>
#include <stdlib.h>

typedef int element;

typedef struct StackNode {
    element data;
    struct StackNode *link;
} StackNode;

typedef struct {
    StackNode *top;
} LinkedStackType;

void init(LinkedStackType *s);
int is_empty(LinkedStackType *s);
int is_full(LinkedStackType *s);
void push(LinkedStackType *s, element item);
element pop(LinkedStackType *s);
element peek(LinkedStackType *s);
void print_stack(LinkedStackType *s);

/* 위 함수의 정의와 본문 전체를 아래에 작성하세요. */
