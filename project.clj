(defproject check-monitor-ssh "0.1.2"
  :description "Check Monitor SSH"
  :url "https://itrsgroup.com"
  :license {:name "ISC License"
            :url "none"
            :year 2019
            :key "isc"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "0.4.2"]
                 [org.clojure/tools.logging "0.5.0"]
                 [clj-logging-config "1.9.12"]
                 [simple-progress "0.1.3"]
                 [me.raynes/fs "1.4.6"]]
  :main check-monitor-ssh.core
  :target-path "target/%s"
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :aliases
  {"native"
   ["shell"
    "native-image"
    "--initialize-at-build-time"
    "--no-fallback"
    ;; "-H:+PrintClassInitialization"
    ;; "-H:+TraceClassInitialization"
    "-H:ReflectionConfigurationFiles=resources/META-INF/native-image/reflect-config.json"
    "-H:+ReportExceptionStackTraces"
    "-jar" "./target/uberjar/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
    "-H:Name=target/${:name}"]}
  :profiles {:uberjar {:aot :all}}
  :plugins [[lein-shell "0.5.0"]])
