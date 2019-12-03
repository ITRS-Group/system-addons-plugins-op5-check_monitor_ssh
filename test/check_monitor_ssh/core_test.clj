(ns check-monitor-ssh.core-test
  (:require [clojure.test :refer :all]
            [me.raynes.fs :as fs]
            [check-monitor-ssh.core :refer :all]
            [check-monitor-ssh.op5.nodes :as op5.n]))

(deftest test-placeholder
  (is (= 0 0)))
