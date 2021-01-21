(defproject check-monitor-ssh "0.1.10"
  :description "Check Monitor SSH"
  :url "https://itrsgroup.com"
  :license {:name "ISC License"
            :url "none"
            :year [2019]
            :key "isc"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.5.0"]
                 [clj-logging-config "1.9.12"]
                 [trptcolin/versioneer "0.2.0"]]
  :main check-monitor-ssh.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :aliases
  {"make-uberjars"
   ["do" ["test"] ["clean"] ["uberjar"]]})
