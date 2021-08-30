# check_monitor_ssh

## Introduction

*check_monitor_ssh* is a Naemon/Nagios plugin to verify SSH connectivity within an OP5 Monitor cluster.

## Usage
Executing `check_monitor_ssh` with the `-h` option will show a simple usage message:

```bash
check_monitor_ssh is a Naemon plugin to verify ssh connectivity within a Merlin
cluster.

The default behavior is to test the connectivity to all nodes in merlin.conf
where the option "connect" is NOT set to "no".

Depending on the result, one of the following exit codes will be given, with
its corresponding Naemon state:

Exit code:   Naemon state:   Available reasons:
-------------------------------------------------------------------------------
0            OK              Successfully connected to all nodes.
                             No nodes to test.
2            CRITICAL        Unable to connect to one or more nodes.
3            UNKNOWN         There was a problem when connecting to one or more
                             nodes. (This is used as a fallback when the error
                             causing the connection error is not recognized.)
-------------------------------------------------------------------------------

Important: Do NOT run as root, but rather as the user monitor. The wrapper
"asmonitor" can be used for this purpose if running the plugin from the
command line.

Usage: check_monitor_ssh [options]

Options:
  -c, --include-connect-no       Also test nodes that has "connect = no" in "merlin.conf".
  -h, --help                     Print this help message.
  -i, --ignore LIST         nil  Ignore the following nodes, comma separated list.
  -t, --timeout INTEGER     10   Seconds before connection times out.
  -v                             Verbosity level; may be specified multiple times to increase value.
  -V, --version                  Print the current version number.

Example output:
"OK: Successfully connected to: poller1,peer2|'Failed SSH Connections'=0;1;1;;"
```

Running the plugin with no options will test the connectivity of all nodes in the cluster and return a message like this:
```bash
monitor@master01:~$ /opt/plugins/check_monitor_ssh 
OK: Successfully connected to all nodes|Failed SSH Connections=0;1;1
```

When using the option `-i` or `--ignore` the specified node(s) are skipped. This is useful if a node is intentionally configured in a way that makes ssh connections impossible.
```bash
monitor@master01:~$ /opt/plugins/check_monitor_ssh -i poller01
OK: Successfully connected to all nodes|Failed SSH Connections=0;1;1
```

Using the `-v` flag will give us more information about what nodes have been connected to.
```bash
monitor@master01:~$ /opt/plugins/check_monitor_ssh -i poller01 -v
OK: Successfully connected to: master02|Failed SSH Connections=0;1;1
```

It's possible to specify several nodes to ignore, using a comma separated list:
```bash
monitor@master01:~$ /opt/plugins/check_monitor_ssh -i poller01,master02 -v
OK: Successfully connected to: poller02|Failed SSH Connections=0;1;1
```

## Build instructions

Building `check_monitor_ssh` only requires a working Docker environment.

### Clone the repository using Git:
```bash
git clone git@github.com:ITRS-Group/system-addons-plugins-op5-check-monitor-ssh.git
``` 

### Run the make file:
```bash
make
```

## Contributors
* [Johan Thorén](https://github.com/johanthoren)

## Security

All releases are signed with the following key which is in turn signed with [my
master key, which is published at keybase.io](https://keybase.io/johan_thoren):

``` public-key
-----BEGIN PGP PUBLIC KEY BLOCK-----

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
U4s4wOukEsEKMlCpugj5gS0f
=4XxZ
-----END PGP PUBLIC KEY BLOCK-----
```

## License

Copyright &copy; 2019-2021 Johan Thorén

This project is licensed under the [GNU General Public License v3.0][license].

[license]: https://choosealicense.com/licenses/gpl-3.0
