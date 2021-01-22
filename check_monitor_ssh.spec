Name: check_monitor_ssh
Version: %{op5version}
Release: %{op5release}%{?dist}
License: ISC
Vendor: ITRS Group
Url: https://www.itrsgroup.com
BuildRoot: %{_tmppath}/%{name}-%{version}
ExclusiveArch: x86_64
Summary: Check Monitor SSH
Prefix: /opt/plugins
Source: %name-%version.tar.gz
BuildRequires: containerd.io
BuildRequires: docker-ce
BuildRequires: docker-ce-cli
Requires: bash
Requires: openssh-clients
Requires: merlin-apps

%description
Naemon plugin to verify ssh connectivity within an OP5 Monitor cluster.

%prep
%setup -q
dockerd &
make

%build

%install
mkdir --parents --mode 755 %{buildroot}%{prefix}
cp target/%name %buildroot%{prefix}/%name

%files
%defattr(755,root,root)
%{prefix}/%name

%clean
rm -rf %{buildroot}

%changelog
* Thu Jan 21 2021 Johan Thorén <jthoren@itrsgroup.com>
- 0.1.10 Fixed problem with encrypted merlin nodes.
* Tue Jan 7 2020 Johan Thorén <jthoren@itrsgroup.com>
- 0.1.9 Added additional documentation.
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
