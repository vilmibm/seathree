; SeaThree, Realtime Twitter Translations
; Copyright (C) 2014 Nathaniel Smith and Benjamin Valentine
;
; This program is free software: you can redistribute it and/or modify
; it under the terms of the GNU Affero General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; This program is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU Affero General Public License for more details.
;
; You should have received a copy of the GNU Affero General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns seathree.handler
  (:gen-class)
  (:require [clojure.tools.nrepl.server :as nrsrv         ]
            [compojure.core             :refer :all       ]
            [cheshire.core              :as json          ]
            [ring.adapter.jetty         :refer (run-jetty)]
            [ring.middleware.gzip       :refer :all       ]
            [ring.middleware.json       :refer :all       ]
            [ring.middleware.params     :refer :all       ]
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

(defn process-params [params]
  (json/parse-string (get params "data") true))

(defroutes router
  ;; Expects ?data="{\"username\"=\"foo\",...}"
  (GET "/tweets-for-user" [:as {p :params}]
       (log/info "request:" p)
       (response (seathree.routes/tweets config (process-params p))))
                                                            
  ;; Expects ?data="[{\"username\"=\"foo\",...},...]"
  (GET "/tweets-for-many" [:as {p :params}]
       (log/info "request:" p)
       (response (map (partial seathree.routes/tweets config) (process-params p))))) 

(def app
  (-> router
      (wrap-params)
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


           
