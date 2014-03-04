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
  (:require [clojure.tools.nrepl.server :as nrsrv           ]
            [compojure.core             :refer :all         ]
            [cheshire.core              :as json            ]
            [liberator.core             :refer [defresource]]
            [liberator.dev              :refer [wrap-trace] ]
            [ring.adapter.jetty         :refer (run-jetty)  ]
            [ring.middleware.gzip       :refer :all         ]
            [ring.middleware.json       :refer :all         ]
            [ring.middleware.params     :refer :all         ]
            [ring.middleware.cors       :refer [wrap-cors]  ]
            [ring.util.response         :refer [response]   ]
            [taoensso.timbre            :as log             ]
            [seathree.config            :as cfg             ]
            [seathree.data              :as data            ]
            [seathree.routes                                ]))

(def default-host "localhost")
(def default-port 8888)
(def default-cfg-file "resources/secrets.clj")
(def default-log-file "/tmp/C3.log")

(def lang-re #"^en|es$") ; TODO add languages

(defn guarded-int [string]
  (if (nil? string) nil (Integer. string)))

(defresource tweets-resource [cfg username src tgt & [since-id]]
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :malformed? (fn [_] (not (every? (partial re-find lang-re) [src tgt])))
  :handle-ok (fn [_] "OKAY"))

(defn web-app [config]
  (routes
   (GET "/tweets/:username/:src/:tgt"
        [username src tgt] (tweets-resource config username src tgt))
   (GET "/tweets/:username/:src/:tgt/:since-id"
        [username src tgt since-id] (tweets-resource config username src tgt (guarded-int since-id)))
   ; TODO static routes
))

(defn get-arg
  "Pull args from maps of the form {\":cli-keyword-arg\" \"value\"
   with optional default"
  [args arg & [default]]
  (or (get args (format "%s" arg)) default))

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

    (let [app (-> (web-app (cfg/get-cfg cfg-file))
                  (wrap-trace :header)
                  (wrap-cors
                   :access-control-allow-origin #".*")
                  (wrap-params)
                  (wrap-gzip))]
      (log/info "STARTUP: starting jetty on" host "port" port)
      (run-jetty app {:port port :host host :join? false}))

    (if nrepl-port
      (do
        (log/info "STARTUP: starting nrepl")
        (def server (nrsrv/start-server :port nrepl-port))))))
