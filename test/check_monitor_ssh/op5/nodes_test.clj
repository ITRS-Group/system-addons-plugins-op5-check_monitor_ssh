(ns check-monitor-ssh.op5.nodes-test
  (:require [check-monitor-ssh.op5.nodes :as n]
            [clojure.test :refer :all]))

(def on-monitor
  (.exists (clojure.java.io/as-file "/etc/op5-monitor-release")))

(deftest test-get-nodeinfo-not-empty
  (testing "Testing that get-nodeinfo is not empty."
    (when on-monitor
      (let [i (n/get-nodeinfo)]
        (is (not-empty i))))))

(deftest test-get-nodeinfo-select-strings
  (testing "Testing that get-nodeinfo contains a number of select strings."
    (when on-monitor
      (let [i (n/get-nodeinfo)
            t #(not-empty (remove nil? (for [l (first i)] (re-find % l))))]
        (is (t #"^    address = 0.0.0.0"))
        (is (t #"^    name = ipc"))
        (is (t #"^ipc"))))))

(deftest test-tidy-nodeinfo-select-strings
  (testing "Testing that tidy-nodeinfo contains a number of select strings."
    (when on-monitor
      (let [i (n/tidy-nodeinfo)
            t #(not-empty (remove nil? (for [l (first i)] (re-find % l))))]
        (is (t #"^address = 0.0.0.0"))
        (is (t #"^name = ipc"))
        (is (t #"^ipc"))))))

(deftest test-split-at-equal
  (testing "Testing that split-at-equal correctly divides strings."
    (let [t #(n/split-at-equal %)]
      (is (= ["1" "2"] (t "1 = 2")))
      (is (= ["1" "2 = 3"] (t "1 = 2 = 3")))
      (is (= ["1" "2"] (t "1 =2")))
      (is (not= ["1" "2"] (t "1=2")))
      (is (not= ["1" "2" "3"] (t "1 = 2 = 3")))
      (is (= ["address" "0.0.0.0"] (t "address = 0.0.0.0")))
      (is (= ["start" "125.432"] (t "start = 125.432")))
      (is (= ["csync_fetch_cmd" nil] (t "csync_fetch_cmd =")))
      (is (= ["cmd" "foo = bar"] (t "cmd = foo = bar")))
      (is (= ["source_name" nil] (t "source_name = (null)"))))))

(deftest test-equals-into-keyvals
  (testing "Testing that strings are correctly mapped."
    (let [t #(n/equals-into-keyvals %1)]
      (is (= {:foo "1"} (t "foo = 1")))
      (is (= {:name "123!"} (t "name = 123!")))
      (is (= {:!strangekey "?"} (t "!strangekey = ?"))))))

(deftest test-nodes
  (testing "Testing that nodes contains certain data."
    (when on-monitor
      (let [n (n/nodes)
            g #(get-in n %&)]
        (is (not-empty n))
        (is (= "ipc" (g :ipc :name)))
        (is (= "0.0.0.0" (g :ipc :address)))
        (is nil? (g :ipc :csync_fetch_cmd))
        (is (> (count (g :ipc :config_hash)) 32))))))
