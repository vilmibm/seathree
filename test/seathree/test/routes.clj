(ns seathree.test.routes
  (:require [clojure.test :refer :all]
            [seathree.data :as data]
            [seathree.routes :as routes]))

(deftest sanity
  (with-redefs [data/get-tweets-from-cache (fn [_ _] [{:hi :there} {:you :guys}])
                data/refresh-tweets! (fn [_ _] nil)]
    (is (= (routes/tweets {} {}) [{:hi :there} {:you :guys}]))))
