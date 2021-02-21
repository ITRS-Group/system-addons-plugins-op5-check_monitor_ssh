(defproject check-monitor-ssh "0.1.11"
  :description "Check Monitor SSH"
  :url "https://itrsgroup.com"
  :license {:name "ISC License"
            :url "none"
            :year [2019]
            :key "isc"}
  :dependencies [[org.clojure/clojure "1.10.2-rc1"]
                 ;;[org.clojure/tools.cli "1.0.194"]
                 ;; Use the SNAPSHOT below which contains the :multi option.
                 ;; When it has been released, this should be removed and the
                 ;; :repositories map should be deleted.
                 [org.clojure/tools.cli "1.0.195-20210214.070531-59"]
                 [org.clojure/tools.logging "1.1.0"]
                 [clj-logging-config "1.9.12"]
                 [trptcolin/versioneer "0.2.0"]]
  :repositories [["sonatype" {:url "https://oss.sonatype.org/service/local/repositories/snapshots/content"
                              ;; If a repository contains releases only setting
                              ;; :snapshots to false will speed up dependencies.
                              :snapshots true
                              ;; Disable signing releases deployed to this repo.
                              ;; (Not recommended.)
                              :sign-releases false
                              ;; You can also set the policies for how to handle
                              ;; :checksum failures to :fail, :warn, or :ignore.
                              :checksum :fail
                              ;; How often should this repository be checked for
                              ;; snapshot updates? (:daily, :always, or :never)
                              :update :always
                              ;; You can also apply them to releases only:
                              :releases {:checksum :fail :update :always}}]]
  :main check-monitor-ssh.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :aliases
  {"make-uberjars"
   ["do" ["test"] ["clean"] ["uberjar"]]})
