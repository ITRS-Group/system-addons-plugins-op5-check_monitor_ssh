Summary: Check Monitor SSH
Name: check_monitor_ssh
Version: 0.1.8
Release: 1
License: ISC
Source: %name-%version.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}
BuildArch: x86_64
Prefix: /opt/plugins
Provides: check_monitor_ssh = %version
Requires: bash
Requires: openssh-clients

%description
A Naemon plugin to verify ssh connectivity within a cluster

%prep
%setup -q

%install
mkdir --parents --mode 755 %{buildroot}%{prefix}
cp target/%name %buildroot%{prefix}/%name

%files
%defattr(755,root,root)
%{prefix}/%name

%changelog
* Tue Dec 10 2019 Johan Thorén <jthoren@itrsgroup.com>
- 0.1.8 Imported tests and updated a couple of functions from ist-collect.
* Fri Dec 06 2019 Johan Thorén <jthoren@itrsgroup.com>
- 0.1.7 Now ignoring nodes with connect = no, unless used with the option -c
* Thu Dec 05 2019 Johan Thorén <jthoren@itrsgroup.com>
- 0.1.6 Changed name to check_monitor_ssh.
- 0.1.5 Added -t (timeout) option.
- 0.1.4 Improved handling of Connection refused errors.
* Wed Dec 04 2019 Johan Thorén <jthoren@itrsgroup.com>
- 0.1.3 Improved error handling overall.
- 0.1.2 Error handling when there is no entry for host in known_hosts.
- 0.1.1 Error handling when no known_hosts is found.
* Tue Dec 03 2019 Johan Thorén <jthoren@itrsgroup.com>
- 0.1.0 Initial commit
