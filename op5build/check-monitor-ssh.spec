Summary: Check Monitor SSH
Name: check-monitor-ssh
Version: 0.1.1
Release: 1
License: ISC
Source: %name-%version.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}
BuildArch: x86_64
Prefix: /opt/plugins
Provides: check-monitor-ssh = %version
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
* Wed Dec 04 2019 Johan Thorén <jthoren@itrsgroup.com>
- 0.1.1 Error handling when no known_hosts is found.
* Tue Dec 03 2019 Johan Thorén <jthoren@itrsgroup.com>
- 0.1.0 Initial commit
