#!/usr/bin/env bats
#
# This function verifies that the string given as argument matches the regex of
# a valid version number.
verify_version_number() {
    grep -P '^(\d{1,}\.\d{1,}\.\d{1,})$' <<< "$1"
}

verify_short_naemon_output() {
    grep -E '^(OK|WARNING|CRITICAL)\|ok=[0-9]{1,2}\swarning=[0-9]{1,2}\serror=[0-9]{1,2}$' <<< "$1"
}

verify_long_naemon_output() {
    grep -E '^(OK|WARNING|CRITICAL)(: )?(Warnings:|Errors)?[^\|]*\|ok=[0-9]{1,2}\swarning=[0-9]{1,2}\serror=[0-9]{1,2}$' <<< "$1"
}

### Begin pre-tests ###

# If 'asmonitor' is not found on PATH it may indicate that later failed tests
# perhaps failed for this reason.
@test "[pre-test] verify that asmonitor is on PATH" {
    run which asmonitor
    [ "$status" -eq 0 ]
}

# If 'mon' is not found on PATH it may indicate that later failed tests
# perhaps failed for this reason.
@test "[pre-test] verify that mon is on PATH" {
    run which mon
    [ "$status" -eq 0 ]
}

### End of pre-tests ###

### Begin main tests ###

@test "invoking /opt/plugins/check_monitor_ssh as root" {
    run /opt/plugins/check_monitor_ssh
    # This is expected to fail with exit code 64.
    [ "$status" -eq 64 ]
    [ "${lines[0]}" = "ERROR: Running this plugin as root is not allowed." ]
}

@test "invoking /opt/plugins/check_monitor_ssh -h as root" {
    run /opt/plugins/check_monitor_ssh -h
    [ "$status" -eq 0 ]
    [ "${lines[0]}" = "check_monitor_ssh is a Naemon plugin to verify ssh connectivity within a Merlin" ]
    [ "${lines[1]}" = "cluster." ]
}

@test "invoking /opt/plugins/check_monitor_ssh -V as root" {
    run /opt/plugins/check_monitor_ssh -V
    [ "$status" -eq 0 ]
    [ "$(verify_version_number "${lines[0]}")" ]
}

@test "invoking /opt/plugins/check_monitor_ssh with no options" {
    run asmonitor /usr/bin//opt/plugins/check_monitor_ssh
    [ "$status" -le 2 ]
}

@test "invoking /opt/plugins/check_monitor_ssh with option -v" {
    run asmonitor /usr/bin//opt/plugins/check_monitor_ssh -v
    [ "$status" -le 2 ]
    [[ "${lines[0]}" == Self-diagnostics\ for* ]]
    [[ "${lines[-1]}" == Self-diagnostics\ for* ]]
}

@test "invoking /opt/plugins/check_monitor_ssh with option -vv" {
    run asmonitor /usr/bin//opt/plugins/check_monitor_ssh -v
    [ "$status" -le 2 ]
    [[ "${lines[0]}" == Self-diagnostics\ for* ]]
    [[ "${lines[-1]}" == Self-diagnostics\ for* ]]
}

@test "invoking /opt/plugins/check_monitor_ssh with option -n" {
    run asmonitor /usr/bin//opt/plugins/check_monitor_ssh -n
    [ "$status" -le 2 ]
    [ "${#lines[@]}" -eq 1 ]
    [ "$(verify_short_naemon_output "${lines[0]}")" ]
    if [ "$status" -eq 0 ]; then
        [[ "${lines[0]}" == OK* ]]
    elif [ "$status" -eq 1 ]; then
        [[ "${lines[0]}" == WARNING* ]]
    elif [ "$status" -eq 2 ]; then
        [[ "${lines[0]}" == CRITICAL* ]]
    fi
}

@test "invoking /opt/plugins/check_monitor_ssh with option -n and -v" {
    run asmonitor /usr/bin//opt/plugins/check_monitor_ssh -n -v
    [ "$status" -le 2 ]
    [ "${#lines[@]}" -eq 1 ]
    [ "$(verify_long_naemon_output "${lines[0]}")" ]
    if [ "$status" -eq 0 ]; then
        [[ "${lines[0]}" == OK* ]]
    elif [ "$status" -eq 1 ]; then
        [[ "${lines[0]}" == WARNING* ]]
    elif [ "$status" -eq 2 ]; then
        [[ "${lines[0]}" == CRITICAL* ]]
    fi
}

@test "invoking /opt/plugins/check_monitor_ssh with the invalid option \"--foo\"" {
    run asmonitor /opt/plugins/check_monitor_ssh --foo
    # This is expected to fail with exit code 3.
    [ "$status" -eq 3 ]
    [ "${lines[0]}" = "[Unknown option: \"--foo\"]" ]
}

### End of main tests ###
