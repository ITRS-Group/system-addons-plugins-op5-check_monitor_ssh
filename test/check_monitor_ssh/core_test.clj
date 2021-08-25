(ns check-monitor-ssh.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [check-monitor-ssh.core :as c]))

(deftest perfdata-string
  (testing "that the correct perfdata string is generated"
    (let [n (inc (rand-int 10))]
      (is (= (c/generate-perfdata-string n)
             (str "|Failed SSH Connections=" n ";1;1"))))))
