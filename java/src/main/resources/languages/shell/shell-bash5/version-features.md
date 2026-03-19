# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Bash 5.x Version Features

## EPOCHSECONDS and EPOCHREALTIME (5.0)

Built-in timestamps without forking to `date`.

```bash
# OLD - fork to date for each timestamp
start_time=$(date +%s)
do_work
end_time=$(date +%s)
elapsed=$((end_time - start_time))

# NEW (5.0) - built-in epoch variables
start_time="${EPOCHSECONDS}"
do_work
end_time="${EPOCHSECONDS}"
elapsed=$((end_time - start_time))

# High-resolution timing
start="${EPOCHREALTIME}"
do_work
end="${EPOCHREALTIME}"
# Note: EPOCHREALTIME includes microseconds (e.g., 1709312456.123456)
```

## wait -n -p (5.1)

Get the PID of the next completed background job, enabling proper parallel job management.

```bash
# OLD - wait for all or specific PID, no way to get which finished first
job1 & pid1=$!
job2 & pid2=$!
wait "${pid1}"
wait "${pid2}"

# NEW (5.1) - wait for any job, get its PID
job1 & pids+=($!)
job2 & pids+=($!)
job3 & pids+=($!)

while ((${#pids[@]} > 0)); do
    wait -n -p finished_pid "${pids[@]}"
    status=$?
    log_info "Job ${finished_pid} exited with status ${status}"
    # Remove finished PID from array
    pids=("${pids[@]/${finished_pid}/}")
done
```

## BASH_ARGV0 (5.0)

Writable variable to change the process name visible in `ps`.

```bash
# OLD - no standard way to change process name
# (some systems support /proc/self/comm)

# NEW (5.0) - writable process name
BASH_ARGV0="my-service: processing"
do_work
BASH_ARGV0="my-service: idle"
```

## Associative Array Improvements (5.0+)

```bash
# OLD - iterate keys and values separately
declare -A config
config[host]="localhost"
config[port]="8080"

for key in "${!config[@]}"; do
    echo "${key}=${config[${key}]}"
done

# NEW (5.0) - ${assoc[@]@K} for key-value pairs
for pair in "${config[@]@K}"; do
    echo "${pair}"
done

# NEW (5.1) - ${var@a} to check variable attributes
declare -A my_map
if [[ "${my_map@a}" == *A* ]]; then
    echo "my_map is an associative array"
fi
```

## -v Test for Array Elements (5.2)

Check existence of specific keys in associative arrays.

```bash
# OLD - check if key exists by testing value
declare -A status_map=(["active"]="1" ["inactive"]="0")
if [[ -n "${status_map[active]+x}" ]]; then
    echo "Key exists"
fi

# NEW (5.2) - direct -v test on array element
declare -A status_map=(["active"]="1" ["inactive"]="0")
if [[ -v 'status_map[active]' ]]; then
    echo "Key exists"
fi

if [[ ! -v 'status_map[unknown]' ]]; then
    echo "Key does not exist"
fi
```

## inherit_errexit (4.4+, improved in 5.x)

Ensure `set -e` propagates into command substitutions.

```bash
# OLD - errexit does NOT propagate into $()
set -e
result=$(false; echo "this still runs")  # "this still runs" executes

# NEW - shopt enables errexit inheritance
shopt -s inherit_errexit
set -e
result=$(false; echo "this never runs")  # subshell exits at false

# Recommended: add to safety header
#!/usr/bin/env bash
set -euo pipefail
shopt -s inherit_errexit
```

## local -n (nameref) (4.3+, stable in 5.x)

Pass variable names to functions for indirect assignment.

```bash
# OLD - eval for indirect assignment (dangerous)
set_result() {
    eval "$1='${2}'"
}

# NEW - nameref for safe indirect assignment
set_result() {
    local -n ref="$1"
    ref="$2"
}

local my_value=""
set_result my_value "hello"
echo "${my_value}"  # prints: hello

# Practical: populate arrays from functions
populate_list() {
    local -n arr="$1"
    arr+=("item1" "item2" "item3")
}

declare -a items=()
populate_list items
echo "${items[@]}"  # prints: item1 item2 item3
```

## mapfile/readarray Improvements (4.4+, stable in 5.x)

```bash
# OLD - while read loop for file-to-array
lines=()
while IFS= read -r line; do
    lines+=("${line}")
done < input.txt

# NEW - mapfile (readarray) for direct loading
mapfile -t lines < input.txt

# With callback for processing
mapfile -t -C 'process_line' -c 1 lines < input.txt

# From command output
mapfile -t running_containers < <(docker ps --format '{{.Names}}')
```

## Recommended Bash 5.x Safety Header

```bash
#!/usr/bin/env bash
set -euo pipefail
shopt -s inherit_errexit
```
