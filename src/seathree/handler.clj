(ns seathree.handler
  (:gen-class)
  (use clojure.tools.logging
       ring.middleware.json
       seathree.core)
  (require [clj-time.core              :as time   ]
           [clojure.tools.nrepl.server :as nrsrv  ]
           [ring.server.standalone     :as server ]
           [seathree.config            :as cfg    ]
           [seathree.twitter           :as twitter]))

;; Update: getting tweets from twitter, storing
;; Sync: request from client for a username
; {username: "foobar", src:"es", tgt: "en"}

(defn exists?
  "Given a map whose values are strings (or sequences) and some
   list of keys, check that the collection has the keys and that
   they are not empty"
  [coll & keys]
  (let [exists?' (fn [x]   (and (contains? coll x) (not (empty? (coll x)))))
        reductor (fn [x y] (and x (exists?' y)))]
    (reduce reductor keys)))

(defn map-keys-to-keywords
  "Given a map, convert its keys to keywords"
  [m]
  (println m)
  (into {}
        (for [[k v] m]
          [(keyword k) v])))

(defn valid?
  "Given a request, validate that it is json and well formed"
  [request]
  (let [content-type (get (:headers request) "content-type")
        body         (map map-keys-to-keywords (:body request))]
    (and
     (= content-type "application/json")
     (every? #(exists? % :username :src :tgt) body))))

(defn success
  "Handler for a successful request."
  [people]
  (future (for [person people]
            (update-last-sync person (time/now))))
  {:status 200
   :body (tweets-for people)})

(defn fail
  "Handler for a failed request. Generic client error (400)."
  []
  {:status 400
   :body "Bad request"})

(defn handler [request]
  (if (valid? request)
    (success (map-keys-to-keywords (:body request)))
    (fail)))

; TODO wrap-gzip
(def app
  (-> handler
      (wrap-json-response)
      (wrap-json-body handler {:keywords? true})))

(defn -main
  "Serve app. Poll Twitter."
  [& args]
  (defonce config (cfg/get-cfg))
  (defonce twitter-creds (twitter/creds-from-cfg config))
  (comment TODO polling)
  ; e.g. (twitter/get-statuses [twitter-creds username since-id 3)
  (defonce server (nrsrv/start-server :port 3333))
  (server/serve app {:open-browser? false})
  (println "Serving."))
