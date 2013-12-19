(ns seathree.handler
  (:gen-class)
  (require [clj-time.core :as time]
           [clojure.tools.nrepl.server :as nrsrv]
           [compojure.core :refer :all]
           [cheshire.core :as json]
           [ring.adapter.jetty :refer (run-jetty)]
           [ring.middleware.gzip :refer :all]
           [taoensso.timbre :as log]
           [seathree.config :as cfg]
           [seathree.data :as data]
           [seathree.routes :as routes]))

(declare config)
(def default-cfg-path "resources/secrets.clj")

(defroutes routes
    (GET "/tweets" [user-data] {:status 200
                                :content-type "application/json"
                                :body (json/generate-string
                                       (routes/tweets config
                                                      (json/parse-string true user-data)))}))

(def app
  (-> routes
      (wrap-json-response)))

(defn -main
  [& args]
  (let [args     (apply array-map args)
        host     (or (get args ":host") "localhost")
        port     (Integer. (or (get args ":port") 8888))
        ws-port  (Integer. (or (get args ":ws-port") 8889))
        cfg-path (or (get args ":config") default-cfg-path)
        log-file (or (get args ":log-file") "/tmp/C3.log")]

    (log/info "STARTUP: Reading config")
    (defonce config (cfg/get-cfg cfg-path))

    (log/info "STARTUP: starting jetty on" host "port" port)
    (run-jetty app {:port port :host host :join? false})

    (log/info "STARTUP: starting nrepl")
    (defonce server (nrsrv/start-server :port 3333))))


           
