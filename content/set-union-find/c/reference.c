#include <stdio.h>
#include <stdlib.h>

#define MAX_VERTICES 100

int parent[MAX_VERTICES];

void set_init(int n)
{
    for (int i = 0; i < n; i++) parent[i] = -1;
}

int set_find(int curr)
{
    if (parent[curr] == -1) return curr;
    return parent[curr] = set_find(parent[curr]);
}

void set_union(int a, int b)
{
    int root1 = set_find(a);
    int root2 = set_find(b);
    if (root1 != root2) parent[root1] = root2;
}
