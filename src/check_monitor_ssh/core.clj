(ns check-monitor-ssh.core
  (:require [clojure.java.shell :as shell]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clj-logging-config.log4j]
            [clojure.string :as str]
            [trptcolin.versioneer.core :refer [get-version]])
  (:gen-class))

(defn set-default-root-logger!
  [loglevel pattern]
  (clj-logging-config.log4j/set-loggers! :root
                                         {:level loglevel
                                          :pattern pattern
                                          :out :console}))

(set-default-root-logger! :fatal "%p: %m%n")

(defn set-log-level!
  "Set the output level based on `verbosity`.
  See also [[set-default-root-logger!]]."
  [verbosity]
  (case verbosity
    0 nil
    1 (set-default-root-logger! :info "%m%n")
    2 (set-default-root-logger! :debug "%m%n")
    (set-default-root-logger! :trace "%d [%p] %c (%t) %m%n")))

(def version-number
  "The version number as defined in project.clj."
  ;; Note that this is evaluated at build time by native-image.
  (get-version "check-monitor-ssh" "check-monitor-ssh"))

(defn generate-perfdata-string
  "A template string for use as perfdata output.
  The number of `failed` connections should be passed as an integer to be
  included in the output."
  [failed]
  (str "|Failed SSH Connections=" failed ";1;1"))

(def exit-messages
  "Exit messages used by `exit`."
  {:64 "ERROR: Running this plugin as root is not allowed."
   :65 (str "OK: No connections to test" (generate-perfdata-string 0))})

(defn exit
  "Print a `message` and exit the program with the given `status` code.
  See also [[exit-messages]]."
  [status message]
  (println message)
  (System/exit status))

(defn current-username?
  "Find out the username of the user running the application."
  []
  (System/getProperty "user.name"))

;; End of general environment functions

(defn bash
  "Run `cmd` as an argument to the shell command 'bash -c'.
  This enables most shell syntax to be preserved and to work as expected."
  [cmd]
  (shell/sh "bash" "-c" cmd))

(defn simple-ssh-command
  "A wrapper to `bash` to execute the `command` remotely instead of locally.
  `host` must be resolvable."
  [host command timeout & {:keys [user] :or {user "monitor"}}]
  (bash (str "ssh -T -o ConnectTimeout=" timeout
             " " user "@" host " " command)))

(defn known-host?
  "Verify that there is an entry for the `host` in the 'known_hosts' file."
  [host]
  (log/trace (str "Looking for " host " in the known_hosts file."))
  (let [known-hosts-file "/opt/monitor/.ssh/known_hosts"]
    (try
      (let [known-hosts (slurp known-hosts-file)
            host-key (re-find (re-pattern host) known-hosts)]
        (log/trace "host-key:" host-key)
        (if (= host host-key)
          {:match true :error nil}
          {:match false :error (str "No entry for " host " in known_hosts")}))
      (catch java.io.FileNotFoundException e
        {:match false :error (str/trim-newline (:cause (Throwable->map e)))}))))

(defn test-ssh-connectivity
  "Perform a basic check of ssh-connectivity to `host`.
  See also [[known-host?]] and [[simple-ssh-command]]."
  [host timeout]
  (log/trace "Testing ssh connectivity for host:" host)
  (let [k (known-host? host)]
    (if-not (:match k)
      {:result false :error (:error k)}
      (let [c (simple-ssh-command host "exit" timeout)]
        (cond
          (zero? (:exit c)) {:result true :error nil}
          :else {:result false :error (str/trim-newline (:err c))})))))

(defn results-from-node
  "Generate a map containing the results for one `node`.
  See also [[test-ssh-connectivity]]."
  [node timeout]
  (let [t (test-ssh-connectivity (:address node) timeout)]
    {:name (:name node)
     :connected (:result t)
     :address (:address node)
     :error (:error t)}))

(defn results-from-cluster
  "Perform `test-ssh-connectivity` tests on all `nodes` in a cluster."
  [nodes timeout]
  (log/trace "Running results-from-cluster on" nodes)
  (let [r (pmap #(results-from-node % timeout) nodes)]
    (if (< (count r) 1) nil r)))

(defn get-nodeinfo
  "Return a string containing the output from 'mon node show'.
  See also [[bash]], [[error-msg]] and [[exit-messages]]."
  []
  (let [c (bash "mon node show")]
    (when (zero? (:exit c))
      (:out c))))

(defn split-at-equal
  "Split a string (`s`) at the first equal sign ('=')."
  [s]
  (let [f (str/split s #"=" 2)
        a (first f)
        b (str/triml (last f))]
    (if (or (= b "") (= b "(null)"))
      [a nil]
      [a b])))

(defn equals-into-keyvals
  "Create a map containing a keyword and a value, extracted from a string
  `s` divided by an equalsign '='.
  See also [[split-at-equal]]."
  [s]
  (let [x (split-at-equal s)]
    (hash-map (keyword (str/lower-case (first x))) (last x))))

(defn partition-mon-node-show
  "Partition the output of in chunks matching the different nodes.
  `output` is expected to be from [[get-nodeinfo]]."
  [output]
  (when (string? output)
    (->> output
         (str/split-lines)
         (remove empty?)
         (partition-by #(str/starts-with? % "#"))
         (partition 2))))

(defn mon-node-keywords
  "Create keywords from the `partitioned-output`, presumably from
  [[get-nodeinfo]]. Each keyword represents a host."
  [partitioned-output]
  (for [n partitioned-output]
    (keyword (last (str/split (ffirst n) #" ")))))

(defn mon-node-values
  "Separate the values from the `partitioned-output`, presumably from
  [[get-nodeinfo]]. Then generate maps based on these lists of values."
  [partitioned-output]
  (for [n partitioned-output]
    (apply merge (map equals-into-keyvals (last n)))))

(defn nodes
  "Join the keywords and values from [[mon-node-keywords]] and
  [[mon-node-values]] into a map.
  See also [[get-nodeinfo]] and [[partition-mon-node-show]]."
  []
  (as-> (get-nodeinfo) <>
        (partition-mon-node-show <>)
        (zipmap (mon-node-keywords <>) (mon-node-values <>))))

(defn filter-out-connect=no
  "Filter out any node in `nodes` that have 'connect = no' in 'merlin.conf'."
  [nodes]
  (log/trace "Removing any nodes that have connect = no.")
  (->> (remove #(= "no" (:connect ((key %) nodes))) nodes)
       (map #(hash-map (first %) (last %)))
       (apply merge)))

(defn filter-out-ignored-nodes
  "Filter out any node in `nodes` specified by the `--ignore` option.
  See also [[cli-options]]."
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

(defn apply-node-filters
  "Filter out any node from `nodes` according to the options specified.
  See also [[filter-out-connect=no]] and [[filter-out-ignored-nodes]]."
  [nodes ignore include-connect=no]
  (if-not include-connect=no
    (filter-out-connect=no (filter-out-ignored-nodes nodes ignore))
    (filter-out-ignored-nodes nodes ignore)))

(defn run-tests!
  "Run `results-from-cluster` on all `nodes`.
  Then filter out the results to populate `successful`, `failed`, and `errors`
  for further logical processing.

  Map the names of the respective results and generate strings to be used with
  any needed error message.

  Generate the perfdata string using `generate-perfdata-string`.

  Then decide which of any potential `errors` that has the highest priority
  and print a message using `critical-error`, also setting the exit code to 2.

  If there were unlisted `errors`, `exit` with code 3 and print the names and
  messages that were gathered while running `results-from-node` on the node in
  question.

  If there were connection `errors` for any node, then `exit` with code 2 and
  print `failed-string`, which includes `failed-names`, together with the
  generated `perfdata`.

  If there were no connection `errors`, then `exit` with code 0 and print a
  `successful-string`, which includes `successful-names`, together with the
  generated `perfdata`."
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
      (exit 3 (if (log/enabled? :info)
                (str "UNKNOWN: Problems connecting to: "
                     errors-names-string ". "
                     (when (log/enabled? :debug)
                       "Errors: " errors-messages-string)
                     perfdata)
                (str "UNKNOWN: Problems occured. Re-run with option -v or -vv"
                     perfdata)))
      ;;
      (seq failed) ; If there are failed nodes.
      (exit 2 (if (log/enabled? :info)
                (str "CRITICAL: Unable to connect to: "
                     failed-string perfdata)
                (str "CRITICAL: Unable to connect to one or more nodes"
                     perfdata)))
      ;;
      :else
      (exit 0 (if (log/enabled? :info)
                (str "OK: Successfully connected to: "
                     successful-string perfdata)
                (str "OK: Successfully connected to all nodes" perfdata))))))

;; Beginning of command line parsing.

(def cli-options
  ;; First three strings describe a short-option, long-option with optional
  ;; example argument description, and a description. All three are optional
  ;; and positional.
  [["-c" "--include-connect-no"
    "Also test nodes that has \"connect = no\" in \"merlin.conf\"."
    :default false]
   ["-h" "--help" "Print this help message." :default false]
   ["-i" "--ignore=LIST" "Ignore the following nodes, comma separated list."
    :default nil]
   ["-t" "--timeout=INTEGER" "Seconds before connection times out."
    :default 10]
   ["-v" nil
    "Verbosity level; may be specified multiple times to increase value."
    :id :verbosity :default 0 :update-fn inc]
   ["-V" "--version" "Print the current version number."
    :default false]])

(defn usage
  "Print a brief description and a short list of available options."
  [options-summary]
  (str/join
   \newline
   ["check_monitor_ssh is a Naemon plugin to verify ssh connectivity within a Merlin"
    "cluster."
    ""
    "The default behavior is to test the connectivity to all nodes in merlin.conf"
    "where the option \"connect\" is NOT set to \"no\"."
    ""
    "Depending on the result, one of the following exit codes will be given, with"
    "its corresponding Naemon state:"
    ""
    "Exit code:   Naemon state:   Available reasons:"
    "-------------------------------------------------------------------------------"
    "0            OK              Successfully connected to all nodes."
    "                             No nodes to test."
    "2            CRITICAL        Unable to connect to one or more nodes."
    "3            UNKNOWN         There was a problem when connecting to one or more"
    "                             nodes. (This is used as a fallback when the error"
    "                             causing the connection error is not recognized.)"
    "-------------------------------------------------------------------------------"
    ""
    "Important: Do NOT run as root, but rather as the user monitor. The wrapper"
    "\"asmonitor\" can be used for this purpose if running the plugin from the"
    "command line."
    ""
    "Usage: check_monitor_ssh [options]"
    ""
    "Options:"
    options-summary
    ""
    "Example output:"
    "\"OK: Successfully connected to: poller1,peer2|'Failed SSH Connections'=0;1;1;;\""]))

(defn validate-args
  "Validate command line arguments.
  Either return a map indicating the program should exit (with a error message,
  and optional ok status), or a map indicating the ticket-id and the options
  provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Exit with a warning if the given ticket-id is not in a valid format.
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      (:version options) ; version => exit OK with version number
      {:exit-message version-number :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message errors}
      ;; custom validation on arguments
      :else
      {:ignore (:ignore options)
       :include-connect-no (:include-connect-no options)
       :timeout (:timeout options)
       :verbosity (:verbosity options)})))

;; End of command line parsing.

(defn -main [& args]
  (let [{:keys [ignore include-connect-no timeout verbosity exit-message ok?]}
        (validate-args args)
        user (current-username?)]
    (set-log-level! verbosity)
    (when exit-message
      (exit (if ok? 0 3) exit-message))
    (when (= "root" user)
      (exit 64 (:64 exit-messages)))
    (when ignore
      (log/trace "Ignoring:" ignore))
    (let [nodes-to-test (apply-node-filters (nodes)
                                            ignore
                                            include-connect-no)]
      (when (or (empty? nodes-to-test)
                (nil? (first nodes-to-test)))
        (exit 0 (:65 exit-messages)))
      (log/trace "Nodes to test:" (keys nodes-to-test))
      (run-tests! nodes-to-test timeout))))
