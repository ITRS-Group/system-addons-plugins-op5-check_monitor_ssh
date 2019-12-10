(ns check-monitor-ssh.op5.nodes
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

;; This file is used to generate a map of potential nodes
;; in the cluster. The only public function is 'nodes', which returns
;; a map of each node with corresponding keywords and values.

(def exit-codes
  ;; The following exit codes may be used by the 'exit' function.
  {:73 "ERROR 73: The program \"mon\" is not installed."})

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn get-nodeinfo
  "Returns a lazy sequence of lists where the first item of each list is
  the name of the node and the rest are its attributes."
  []
  (try
    (->> (shell/sh "asmonitor" "mon" "node" "info")
         (:out)
         (str/split-lines)
         (partition-all 67))
    (catch java.io.IOException e
      (exit 3 (:73 exit-codes)))))

(defn tidy-nodeinfo
  "Returns a lazy sequence of lists where the output from get-nodeinfo is
  without empty lines and whitespace."
  []
  (for [col (get-nodeinfo)]
    (remove empty? (for [s col]
                     (str/trim s)))))

(defn split-at-equal
  "Splits a string at the first equal sign preceeded by a space."
  [s]
  (let [f (str/split s #" =" 2)
        a (first f)
        b (str/triml (last f))]
    (cond
      (= b "") [a nil]
      (= b "(null)") [a nil]
      :else [a b])))

(defn equals-into-keyvals
  "Creates a map containing a keyword and a value, extracted from a string
  divided by an equalsign."
  [s]
  (let [x (split-at-equal s)]
    (hash-map (keyword (first x)) (last x))))

(defn nodes
  "Returns a map of nodes with their corresponding maps of attributes."
  []
  (apply merge (for [col (tidy-nodeinfo)]
                 (hash-map (keyword (first col))
                           (apply merge (for [s (rest col)]
                                          (equals-into-keyvals s)))))))
