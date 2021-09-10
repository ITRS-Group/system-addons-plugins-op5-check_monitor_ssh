#!/usr/bin/env bats
export ASSERTION_SOURCE="$(pwd)/test/check_monitor_ssh/bats/assertions"
load "$(pwd)/test/check_monitor_ssh/bats/assertion-test-helpers"

### Begin main tests ###

@test "invoking check_monitor_ssh with the invalid option \"--foo\"" {
    run ./check_monitor_ssh --foo
    assert_status 3
    assert_line_equals 0 "Unknown option: \"--foo\""
}

@test "invoking check_monitor_ssh with the invalid argument 200" {
    run ./check_monitor_ssh 200
    assert_status 66
    assert_line_matches 0 ERROR
}

@test "invoking check_monitor_ssh with the invalid argument \"foo\"" {
    run ./check_monitor_ssh foo
    assert_status 66
    assert_line_matches 0 ERROR
}

@test "invoking check_monitor_ssh with the invalid arguments \"foo\" 1" {
    run ./check_monitor_ssh foo 1
    assert_status 66
    assert_line_matches 0 ERROR
}

@test "invoking check_monitor_ssh with options -V and argument" {
    run ./check_monitor_ssh -V foo
    assert_status 66
    assert_line_matches 0 ERROR
}

@test "invoking check_monitor_ssh with options -h and argument" {
    run ./check_monitor_ssh -h foo
    assert_status 66
    assert_line_matches 0 ERROR
}

@test "invoking check_monitor_ssh with options -vv and argument" {
    run ./check_monitor_ssh -vv 2021
    assert_status 66
    assert_line_matches 0 ERROR
}

@test "invoking check_monitor_ssh -V" {
    run ./check_monitor_ssh -V
    assert_status 0
    assert_output_matches "^[0-9]+\.[0-9]+\.[0-9]+-?(SNAPSHOT)?$"
}

@test "invoking check_monitor_ssh -h" {
    run ./check_monitor_ssh -h
    assert_status 0
    assert_line_equals 0 "check_monitor_ssh is a Naemon plugin to verify ssh connectivity within a Merlin"
    assert_line_equals 1 "cluster."
}

@test "invoking check_monitor_ssh with option -vvv" {
    run ./check_monitor_ssh -vvv
    assert_status 0
    assert_line_matches 0 TRACE
}

@test "invoking check_monitor_ssh with no option or argument" {
    run ./check_monitor_ssh
    assert_status 0
    assert_line_matches 0 OK
}

# ### End of main tests ###
