(ns seathree.test.data
  (:require [clojure.test :refer :all]
            [twitter.oauth :as oauth]
            [seathree.data :as data]))

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
    (testing "passes arguments"
      (let [call-args  (atom [])
            fn         (fn [& args] (swap! call-args (fn [_] args)))]
        (data/with-retries 4 fn :foo :bar)
      (is (= @call-args [:foo :bar])))))
    (testing "retries correct number of times")
    (testing "returns proper value"))

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


(deftest get-tweets-from-twitter)

(deftest gen-key')

(deftest store-ts!)

(deftest get-ts)

(deftest translate)

(deftest refresh-tweets!)

(deftest get-tweets-from-cache)

