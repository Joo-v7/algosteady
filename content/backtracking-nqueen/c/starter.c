#include <stdio.h>
#include <stdlib.h>

#define MAX_N 10

// 같은 열·대각선 충돌 검사와 배치·복구 재귀 구현
int is_safe(const int columns[], int row, int column);
int solve_n_queens(int n, int row, int columns[]);
