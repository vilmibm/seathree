(ns seathree.test.data
  (:require [clojure.test :refer :all]
            [clj-time.core :as time]
            [clj-time.format :as tfmt]
            [taoensso.carmine :as car]
            [twitter.oauth :as oauth]
            [seathree.data :as data]))

(def user-data {:username "nate_smith" :src "en" :tgt "es"})
(def _ nil)

(defn fn-lift
  [value]
  (fn [& args] value))

(deftest process-failed?
  (testing "process failed"
    (is (= true (data/process-failed? {:exit 1}))))

  (testing "process didn't fail"
    (is (= false (data/process-failed? {:exit 0})))))

(deftest match
  (testing "returns true for a match"
    (is (= true (data/match #"^foob.*az$" "foobarbaaaaaz"))))
  (testing "returns false for not a match"
    (is (= false (data/match #"^foob.*az$" "foobarbz")))))

(deftest with-retries
  (testing "ignores retries < 0"
    (let [call-count (atom 0)
          fn         #(swap! call-count inc)
          result     (data/with-retries -1 fn)]
      (is (nil? result))
      (is (= 0 @call-count))))
  (testing "does nothing for 0"
    (let [call-count (atom 0)
          fn         #(swap! call-count inc)
          result     (data/with-retries 0 fn)]
      (is (nil? result))
      (is (= 0 @call-count))))
  (testing "for > 0"
    (testing "retries correct number of times and returns proper value"
      (let [call-count (atom 0)
            fn         (fn [] (swap! call-count inc) (if (= @call-count 4) :foo nil))
            result     (data/with-retries 10 fn)]
        (is (= result :foo))
        (is (= @call-count 4))))))

(deftest building-twitter-creds
  (testing "calls oauth function with proper args in proper order"
    (let [call-args  (atom [])
          mock-oauth (fn [& args] (swap! call-args (fn [_] args)))
          mock-cfg   {:twitter {:oauth {:access-token "789"
                                        :consumer-secret "456"
                                        :consumer-key "123"
                                        :access-token-secret "000"}}}]
      (with-redefs [oauth/make-oauth-creds mock-oauth]
        (data/twitter-creds-from-cfg mock-cfg)
        (is (= @call-args ["123" "456" "789" "000"]))))))

(deftest gen-key'
  (is (= "tweets_nate_smith_en_es" (data/gen-key' :tweets user-data))))

(deftest store-ts!
  (testing "with timestamp"
    (let [call-args    (atom [])
          redis-called (atom false)
          now-called   (atom false)]
      (with-redefs [data/redis   (fn [_ fn] (swap! redis-called (fn-lift true)) (fn))
                    time/now     (fn [] (swap! now-called (fn-lift true)))
                    tfmt/unparse (fn [f t] t)
                    car/set      (fn [k v] (swap! call-args (fn-lift [k v])))]
        (data/store-ts! {} (fn-lift "foo") {} "barbaz")
        (is (= true @redis-called))
        (is (= false @now-called))
        (is (= @call-args ["foo" "barbaz"])))))
  (testing "without timestamp"
    (let [call-args    (atom [])
          redis-called (atom false)
          now-called   (atom false)]
      (with-redefs [data/redis   (fn [_ fn] (swap! redis-called (fn-lift true)) (fn))
                    time/now     (fn [] (swap! now-called (fn-lift true)) "barbaz")
                    tfmt/unparse (fn [f t] t)
                    car/set      (fn [k v] (swap! call-args (fn-lift [k v])))]
        (data/store-ts! {} (fn-lift "foo") {})
        (is (= true @redis-called))
        (is (= true @now-called))
        (is (= @call-args ["foo" "barbaz"]))))))

(deftest get-ts
  (testing "handles non-nil"
    (with-redefs [time/minus (fn-lift "foo")
                  tfmt/parse (fn-lift "bar")
                  data/redis (fn-lift "ts")]
      (let [result (data/get-ts _ (fn-lift "baz") _)]
        (is (= result "bar")))))
  (testing "handles nil"
    (with-redefs [time/minus (fn-lift "foo")
                  tfmt/parse (fn-lift "bar")
                  data/redis (fn-lift nil)]
      (let [result (data/get-ts _ (fn-lift "baz") _)]
        (is (= result "foo"))))))

(deftest get-tweets-from-cache
  (let [call-args (atom [])]
    (with-redefs [data/redis (fn [_ f] (f))
                  car/llen   (fn-lift 100)
                  car/lrange (fn [& args] (swap! call-args (fn-lift args)) [:tweets-here])]
      (let [result (data/get-tweets-from-cache _ user-data)]
        (is (= @call-args ["tweets_nate_smith_en_es" 0 100]))
        (is (= result (assoc user-data :tweets [:tweets-here])))))))


(deftest get-tweets-from-twitter)

(deftest translate)

(deftest refresh-tweets!
  (let [now           (time/now)
        stale         (time/minutes 5)] ;; pin this so tests can hardcode against 5
    (testing "when data is fresh"
      (let [redis-called (atom false)]
        (with-redefs [data/stale  stale 
                      data/get-ts (fn-lift (time/minus now (time/minutes 1)))
                      time/now    now
                      data/redis  (fn [_ _] (swap! redis-called (fn-lift true)))]
          (data/refresh-tweets! _ user-data)
          (is (= false @redis-called) "redis is not called"))))
    (testing "when data is stale"
      (let [store-ts-called (atom false)
            lpush-args      (atom [])]
      (with-redefs [data/stale                   stale
                    data/get-ts                  (fn-lift (time/minus now (time/minutes 6)))
                    time/now                     now
                    data/redis                   (fn [_ f] (f))
                    data/store-ts!               (fn [_ _ _] (swap! store-ts-called (fn-lift true)))
                    car/lindex                   (fn [_ _] {:id 123})
                    car/lpush                    (fn [_ tweets] (swap! lpush-args (fn-lift tweets)))
                    data/get-tweets-from-twitter (fn-lift ["one" "two" "three"])
                    data/translate               (fn-lift "four")]
        (data/refresh-tweets! _ user-data)
        (comment TODO))))

    (testing "when twitter API fails")
    (testing "when translation fails")))

