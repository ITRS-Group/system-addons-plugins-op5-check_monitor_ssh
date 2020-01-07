# Introduction to check_monitor_ssh

## Simple usage
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
1            WARNING         This state is currently NOT IN USE.
2            CRITICAL        Unable to connect to one or more nodes.
3            UNKNOWN         There was a problem when connecting to one or more
                             nodes. (This is used as a fallback when the error
                             causing the connection error is not recognized.)
-------------------------------------------------------------------------------

Important: Do NOT run as root, but rather as the user monitor. The wrapper
"asmonitor" can be used for this purpose.

Usage: check_monitor_ssh [options]

Options:
  -c, --include-connect-no       Also test nodes that has "connect = no" in "merlin.conf"
  -d, --debug                    Set the verbosity level to debug, use only for debugging
  -h, --help                     Print this help message
  -i, --ignore LIST         nil  Ignore the following nodes, comma separated list
  -t, --timeout INTEGER     10   Seconds before connection times out

Example output:
"OK: Successfully connected to: poller1,peer2|'Failed SSH Connections'=0;1;1;;"
```

Running the plugin with no options will test the connectivity of all nodes in the cluster and return a message like this:
```bash
monitor@master01:~$ /opt/plugins/check_monitor_ssh 
OK: Successfully connected to: poller01,master02|'Failed SSH Connections'=0;1;1;;
```

When using the option `-i` or `--ignore` the specified node(s) are skipped. This is useful if a node is intentionally configured in a way that makes ssh connections impossible.
```bash
monitor@master01:~$ /opt/plugins/check_monitor_ssh -i poller01
OK: Successfully connected to: master02|'Failed SSH Connections'=0;1;1;;
```

It's possible to specify several nodes to ignore, using a comma separated list:
```bash
monitor@master01:~$ /opt/plugins/check_monitor_ssh -i poller01,master02
OK: Successfully connected to: poller02|'Failed SSH Connections'=0;1;1;;
```

## Build instructions

**Important:** as long as `check_monitor_ssh` should work for EL6, it also needs to be built on EL6. If built on EL7, it will not run on EL6 due to missing libc dependency.

Building requires `graal-vm` to be installed with the module `native-image`.

### Clone the repository using Git:
```bash
git clone git@github.com:ITRS-Group/system-addons-plugins-op5-check-monitor-ssh.git
``` 

### Download and install Leiningen:
```bash
mkdir -p ~/bin
curl -O https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein ~/bin/
chmod a+x ~/bin/lein
~/bin/lein
```

### Further instructions:
Make sure `~/bin` is on your `$PATH` to be able to run `lein` without specifying the whole path.

For more information on installing Leiningen, please see [leiningen.org](https://leiningen.org/ "Leiningen's website").

After `lein` has been installed, enter the directory where you cloned the repo and run the following:
```bash
lein uberjar
```

After the uberjar process has finished, build the standalone binary with:
```bash
lein native
```

This will build the project and create a binary file named `target/check_monitor_ssh`. The file `check_monitor_ssh` can now be executed or packaged in an RPM.
