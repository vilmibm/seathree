(ns seathree.test.handler
  (:require [clojure.test               :refer :all       ]
            [seathree.test.helpers      :refer :all       ]
            [clojure.tools.nrepl.server :as nrsrv         ]
            [taoensso.timbre            :as log           ]
            [ring.adapter.jetty         :refer [run-jetty]]
            [seathree.config            :as cfg           ]
            [seathree.handler           :as handler       ]))

(deftest get-arg
  (let [args {":hi" "there" ":you" "guys"}]
    (testing "returns default if nil and default provided"
      (is (= "are" (handler/get-arg args :how "are"))))
    (testing "returns nil if nil and no default"
      (is (= nil (handler/get-arg args :how))))
    (testing "returns arg"
      (is (= "guys" (handler/get-arg args :you))))))

(deftest guarded-int
  (testing "nil if nil"
    (is (= nil (handler/guarded-int nil))))
  (testing "number otherwise"
    (is (= 123 (handler/guarded-int 123)))))
    

(deftest main
  (with-redefs [log/set-config! nil-fn
                log/info        nil-fn]
    (testing "uses defaults"
      (let [jetty-args (atom [])
            cfg-arg    (atom nil)]
        (with-redefs [handler/app        "app"
                      cfg/get-cfg        (fn [path] (swap! cfg-arg (fn-lift path)))
                      run-jetty          (fn [& args] (swap! jetty-args (fn-lift args)))
                      nrsrv/start-server nil-fn]
        (handler/-main)
        (is (= @jetty-args ["app" {:port handler/default-port :host handler/default-host :join? false}]))
        (is (= @cfg-arg handler/default-cfg-file)))))

    (testing "accepts proper args"
      (let [jetty-args (atom [])
            cfg-arg    (atom nil)
            nrepl-arg  (atom nil)]
        (with-redefs [handler/app        "app"
                      cfg/get-cfg        (fn [path] (swap! cfg-arg (fn-lift path)))
                      run-jetty          (fn [& args] (swap! jetty-args (fn-lift args)))
                      nrsrv/start-server (fn [_ port] (swap! nrepl-arg (fn-lift port)))]
          (handler/-main ":host" "foo" ":port" "123" ":config" "bean/bar" ":nrepl-port" "456")
          (is (= @jetty-args ["app" {:port 123 :host "foo" :join? false}]))
          (is (= @nrepl-arg 456))
          (is (= @cfg-arg "bean/bar")))))

    (testing "doesn't run nrepl if no nrepl port"
      (let [nrepl-called (atom false)]
        (with-redefs [handler/app        "app"
                      nrsrv/start-server (fn [& a] (swap! nrepl-called true-fn))
                      run-jetty          nil-fn
                      cfg/get-cfg        nil-fn]
        (handler/-main)
        (is (= false @nrepl-called)))))))
