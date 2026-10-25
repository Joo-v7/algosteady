#include <string.h>
#include "submission.c"

static void fail(void)
{
    fprintf(stderr, "활동 ID·겹침·반환 개수·원본 보존 검사 오류\n");
    exit(2);
}

int main(void)
{
    Activity guarded[MAX_ACTIVITIES + 2];
    Activity original[MAX_ACTIVITIES];
    Activity *activities = &guarded[1];
    int n = -1;
    char command[16];
    while (scanf("%15s", command) == 1) {
        if (strcmp(command, "SELECT") == 0) {
            if (scanf("%d", &n) != 1 || n < 0 || n > MAX_ACTIVITIES) return 2;
            for (int i = 0; i < MAX_ACTIVITIES + 2; i++) guarded[i] = (Activity){-987654321, -987654321, -987654321};
            for (int i = 0; i < n; i++) {
                int start;
                int end;
                if (scanf("%d %d", &start, &end) != 2 || start < -1000000 || end > 1000000 || start >= end) return 2;
                activities[i] = (Activity){i, start, end};
                original[i] = activities[i];
            }
        } else if (strcmp(command, "AGAIN") != 0 || n < 0) return 2;

        int output[MAX_ACTIVITIES + 2];
        for (int i = 0; i < MAX_ACTIVITIES + 2; i++) output[i] = -987654321;
        int *selected = &output[1];
        int count = select_activities(activities, n, selected);
        if (count < 0 || count > n) fail();
        for (int i = 0; i < MAX_ACTIVITIES + 2; i++) {
            if (i >= 1 && i <= n) continue;
            if (output[i] != -987654321 || guarded[i].id != -987654321
                    || guarded[i].start != -987654321 || guarded[i].end != -987654321) fail();
        }

        // 입력 순서 변경은 허용하되 ID·시작·종료 값 보존 확인
        int seen[MAX_ACTIVITIES] = {0};
        for (int i = 0; i < n; i++) {
            Activity activity = activities[i];
            if (activity.id < 0 || activity.id >= n || seen[activity.id]) fail();
            if (activity.start != original[activity.id].start || activity.end != original[activity.id].end) fail();
            seen[activity.id] = 1;
        }
        memset(seen, 0, sizeof(seen));
        int last_end = 0;
        for (int i = 0; i < count; i++) {
            int id = selected[i];
            if (id < 0 || id >= n || seen[id]) fail();
            if (i > 0 && original[id].start < last_end) fail();
            seen[id] = 1;
            last_end = original[id].end;
        }
        printf("SELECTED %d\n", count);
    }
    return 0;
}
