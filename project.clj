(defproject check-monitor-ssh "0.1.11"
  :description "Check Monitor SSH"
  :url "https://itrsgroup.com"
  :license {:name "GPL-3.0"
            :url "https://choosealicense.com/licenses/gpl-3.0"
            :year [2019]
            :key "gpl-3.0"
            :comment "GNU General Public License v3.0"}
  :dependencies [[org.clojure/clojure "1.10.2-rc1"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.1.0"]
                 [clj-logging-config "1.9.12"]
                 [trptcolin/versioneer "0.2.0"]]
  :main check-monitor-ssh.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :aliases
  {"make-uberjars"
   ["do" ["test"] ["clean"] ["uberjar"]]}
  :release-tasks [["test"]
                  ["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
