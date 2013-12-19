(ns seathree.handler
  (:gen-class)
  (:require [clojure.tools.nrepl.server :as nrsrv         ]
            [compojure.core             :refer :all       ]
            [cheshire.core              :as json          ]
            [ring.adapter.jetty         :refer (run-jetty)]
            [ring.middleware.gzip       :refer :all       ]
            [ring.middleware.json       :refer :all       ]
            [ring.util.response         :refer [response] ]
            [taoensso.timbre            :as log           ]
            [seathree.config            :as cfg           ]
            [seathree.data              :as data          ]
            [seathree.routes                              ]))

(declare config)
(def default-host "localhost")
(def default-port 8888)
(def default-cfg-file "resources/secrets.clj")
(def default-log-file "/tmp/C3.log")

(defroutes router
    (GET "/tweets-for-user" [user-data] (response (seathree.routes/tweets config (json/parse-string true user-data))))
    (GET "/tweets-for-many" [user-data-list] (response
                                              (map (partial seathree.routes/tweets config)
                                                   (json/parse-string true user-data-list)))))

(def app
  (-> router
      (wrap-json-response)
      (wrap-gzip)))

(defn get-arg
  "Pull args from maps of the form {\":cli-keyword-arg\" \"value\"
   with optional default"
  [args arg & [default]]
  (or (get args (format "%s" arg)) default))

(defn guarded-int [string]
  (if (nil? string)
    nil
    (Integer. string)))

(defn -main
  [& args]
  (let [get-arg    (partial get-arg (apply array-map args))
        port       (guarded-int (get-arg :port default-port))
        nrepl-port (guarded-int (get-arg :nrepl-port))
        host       (get-arg :host default-host)
        cfg-file   (get-arg :config default-cfg-file)
        log-file   (get-arg :log-file default-log-file)]

    (log/set-config! [:timestamp-pattern] "yyyy-MM-dd HH:mm:ss ZZ")
    (log/set-config! [:appenders :spit :enabled?] true)
    (log/set-config! [:shared-appender-config :spit-filename] log-file)
    
    (log/info "STARTUP: Reading config")
    (def config (cfg/get-cfg cfg-file))

    (log/info "STARTUP: starting jetty on" host "port" port)
    (run-jetty app {:port port :host host :join? false})

    (if nrepl-port
      (do
        (log/info "STARTUP: starting nrepl")
        (def server (nrsrv/start-server :port nrepl-port))))))


           
