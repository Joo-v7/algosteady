#!/bin/sh

# 사용자 프로그램의 stderr와 timeout 진단을 분리한다.
seconds=$1
shift
rm -f /work/result /work/timeout-error /work/timeout.pid
exec 3>&2
exec 4<&0
start=$(date +%s%N)
timeout --verbose --signal=KILL "${seconds}s" sh -c 'exec "$@" 2>&3' runner "$@" <&4 2>/work/timeout-error &
pid=$!
printf '%s' "$pid" > /work/timeout.pid
wait "$pid" 2>/dev/null
code=$?
end=$(date +%s%N)
timed_out=0
if test -s /work/timeout-error; then timed_out=1; fi
printf '%s %s %s' "$code" "$((end-start))" "$timed_out" > /work/result
