(ns seathree.test.config
  (:require [clojure.test :refer :all]
            [seathree.config :as cfg]))

(def config {:hi "there" :how {:are "you"}})

(deftest evals-form
  (let [evaled-config (with-redefs [slurp (fn [_] "{:hi \"there\" :how {:are \"you\"}}")]
                        (cfg/get-cfg "foo"))]
    (is (= config evaled-config)))) 

(deftest foo
  (is (= 1 1)))
