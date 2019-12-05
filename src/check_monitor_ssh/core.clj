(ns check-monitor-ssh.core
  (:require [clojure.java.shell :as shell]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clj-logging-config.log4j]
            [clojure.string :as str]
            [check-monitor-ssh.op5.nodes :as op5.n])
  (:gen-class))

(defn set-default-root-logger!
  [loglevel pattern]
  (clj-logging-config.log4j/set-loggers! :root
                                         {:level loglevel
                                          :pattern pattern
                                          :out :console}))

(set-default-root-logger! :info "%p: %m%n")

(defn generate-perfdata-string
  "A template string for use as perfdata output."
  [failed]
  (str "|'Failed SSH Connections'=" failed ";1;1;;"))

(def exit-codes
  "The following exit codes may be used by the 'exit' function."
  {:64 "UNKNOWN: Running this plugin as root is not allowed."
   :65 (str "OK: No connections to test" (generate-perfdata-string 0))})

(defn exit
  "Prints a message and exits the program with the given status code."
  [status msg]
  (println msg)
  (System/exit status))

(defn current-username?
  []
  (System/getProperty "user.name"))

;; End of general environment functions

;; Command line parsing below. The copyright notice pertains
;; only to this section.

;; Based on example given at https://github.com/clojure/tools.cli#example-usage
;; Copyright (c) Rich Hickey and contributors. All rights reserved.

;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.

;; You must not remove this notice, or any other, from this software.

(def cli-options
  ;; First three strings describe a short-option, long-option with optional
  ;; example argument description, and a description. All three are optional
  ;; and positional.
  [["-d" "--debug" "Sets log level to debug" :id :debug? :default false]
   ["-h" "--help" "Print this help message" :default false]
   ["-i" "--ignore=LIST" "Ignore the following nodes, comma separated list"
    :default nil]
   ["-t" "--timeout=INTEGER" "Seconds before connection times out"
    :default 10]])

(defn usage
  "Prints a brief description and a short list of available options."
  [options-summary]
  (str/join
   \newline
   [(str "check_monitor_ssh is a Naemon plugin to verify ssh connectivity "
         "within a cluster.")
    ""
    "Usage: check_monitor_ssh [options]"
    ""
    "Options:"
    options-summary]))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the ticket-id and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Exit with a warning if the given ticket-id is not in a valid format.
    (log/debug "The following arguments will be processed as modules:" arguments)
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message errors}
      ;; custom validation on arguments
      :else
      {:debug? (:debug? options)
       :ignore (:ignore options)
       :timeout (:timeout options)})))

;; End of command line parsing.

;; The following functions are related to remote connections.

(defn bash
  "Run cmd as an argument to 'bash -c'. This enables most shell
  features to remain inside the commands being passed."
  [cmd]
  (shell/sh "bash" "-c" cmd))

(defn simple-ssh-command
  "This is simply a wrapper of the bash function to execute the command
  remotely instead. Host must be resolvable."
  [host command timeout & {:keys [user] :or {user "monitor"}}]
  (bash (str "ssh -T -o ConnectTimeout=" timeout
             " " user "@" host " " command)))

(defn known-host?
  "Checks to see if there is an entry for the host in the 'known_hosts' file."
  [host]
  (let [known-hosts-file "/opt/monitor/.ssh/known_hosts"]
    (try
      (let [known-hosts (slurp known-hosts-file)
            host-key (re-find (re-pattern host) known-hosts)]
        (if (= host host-key)
          {:match true :error nil}
          {:match false :error (str "No entry for " host " in known_hosts")}))
      (catch java.io.FileNotFoundException e
        {:match false :error (str/trim-newline (:cause (Throwable->map e)))}))))

(defn test-ssh-connectivity
  "Performs a basic check of ssh-connectivity and environment."
  [host timeout]
  (log/debug "Testing ssh connectivity for host:" host)
  (let [k (known-host? host)]
    (if-not (:match k)
      {:result false :error (:error k)}
      (let [c (simple-ssh-command host "exit" timeout)]
        (cond
          (zero? (:exit c)) {:result true :error nil}
          :else {:result false :error (str/trim-newline (:err c))})))))

(defn results-from-node
  "Generate a map containing the results for one node."
  [node timeout]
  (let [t (test-ssh-connectivity (:address node) timeout)]
    {:name (:name node)
     :connected (:result t)
     :address (:address node)
     :error (:error t)}))

(defn results-from-cluster
  "Needs a collection of node(s) to iterate over."
  [nodes timeout]
  (log/debug "Running results-from-cluster on" nodes)
  (let [r (for [n nodes] (results-from-node n timeout))]
    (if (< (count r) 1) nil r)))

(defn filter-out-ignored-hosts
  [nodes ignore]
  (if-not ignore          ; If there is nothing to ignore,
    (dissoc nodes :ipc)   ; just ignore the local node.
    (as-> ignore <>
      (str <>)
      (str/split <> #",") ; Divide the comma delimited string.
      (do
        ;; Before moving on, make sure that there are no typos or invalid
        ;; nodes given as an argument to --ignore. The rationale for this
        ;; is to avoid silent errors.
        (doseq [i <>]
          (when-not (contains? nodes (keyword i))
            (exit 66 (str "ERROR: No such node: " i))))
        (map keyword <>)) ; Make keywords out of the items.
      (conj <> :ipc)      ; Add the local node to the list and
      (apply dissoc nodes <>)))) ; delete them all, returning a new map.

(defn run-tests!
  [nodes timeout]
  (let [results (results-from-cluster (vals nodes) timeout)
        successful (filter :connected results)
        failed (remove :connected results)
        errors (filter :error results)
        successful-names (map :name successful)
        failed-names (map :name failed)
        errors-names (map :name errors)
        errors-messages (map :error errors)
        failed-string (if (> (count failed) 1)
                        (str/join "," failed-names)
                        (first failed-names))
        successful-string (if (> (count successful) 1)
                            (str/join "," successful-names)
                            (first successful-names))
        errors-names-string (if (> (count errors) 1)
                              (str/join "," errors-names)
                              (first errors-names))
        errors-messages-string (if (> (count errors) 1)
                                 (str/join ", " errors-messages)
                                 (first errors-messages))
        perfdata (generate-perfdata-string (count failed))
        filter-error #(and (seq errors)
                           (seq (remove nil? (for [m errors-messages]
                                               (re-find % m)))))
        critical-error #(exit 2 (str "CRITICAL: " % " for: "
                                     errors-names-string "." perfdata))]
    (cond
      (filter-error #"Connection refused")
      (critical-error "Connection refused")
      ;;
      (filter-error #"Connection timed out")
      (critical-error "Connection timed out")
      ;;
      (filter-error #"Could not resolve")
      (critical-error "Could not resolve")
      ;;
      (seq errors) ; If there are error messages not caught by the filters.
      (exit 3 (str "UNKNOWN: Problems connecting to: "
                   errors-names-string ". "
                   "Errors: " errors-messages-string
                   perfdata))
      ;;
      (seq failed) ; If there are failed nodes.
      (exit 2 (str "CRITICAL: Unable to connect to: "
                   failed-string perfdata))
      ;;
      :else
      (exit 0 (str "OK: Successfully connected to: "
                   successful-string perfdata)))))

;; End of functions relating to remote connections.

(defn -main [& args]
  (let [{:keys [debug? ignore timeout exit-message ok?]} (validate-args args)
        user (current-username?)]
    (when debug?
      (set-default-root-logger! :debug "%d [%p] %c (%t) %m%n"))
    (log/debug "Ignoring:" ignore)
    (when (= "root" user)
      (exit 3 (:64 exit-codes)))
    (log/debug "Running check_monitor_ssh as user" user)
    (when exit-message
      (exit (if ok? 0 3) exit-message))
    (let [all-nodes (op5.n/nodes)
          nodes-to-test (filter-out-ignored-hosts all-nodes ignore)]
      (when (or (empty? nodes-to-test)
                (nil? (first nodes-to-test)))
        (exit 0 (:65 exit-codes)))
      (log/debug "Nodes to test:" (keys nodes-to-test))
      (run-tests! nodes-to-test timeout))))
