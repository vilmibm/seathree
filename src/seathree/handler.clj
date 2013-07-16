(ns seathree.handler
  (:gen-class)
  (use clojure.tools.logging
       ring.middleware.json
       seathree.core)
  (require [clojure.data.json          :as json   ] ; TODO look into cheshire
           [clojure.tools.nrepl.server :as nrsv   ]
           [seathree.config            :as cfg    ]
           [seathree.twitter           :as twitter]))

(comment TODO gzipping)
(comment TODO re-architect to work with status ids instead of update timestamps!)

(defn valid?
  "TODO"
  [request]
  (let [content-type (get (:headers request) "content-type")
        body         (:body request)]
    (and
     (= content-type "application/json")
     (not (empty? (:usernames body))))))

(defn extract-usernames
  "TODO"
  [request]
  (:usernames (:body request)))

(defn success
  "TODO"
  [usernames]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str (tweets-for usernames))})

(defn fail
  "TODO"
  []
  {:status 400
   :body "Bad request"})

(defn handler [request]
  (if (valid? request)
    (success (extract-usernames request))
    (fail)))

(def app
  (wrap-json-body handler {:keywords? true}))

(def -main
  "Serve app. Poll Twitter."
  [& args]
  (defonce config (cfg/get-cfg))
  (defonce twitter-creds (twitter/twitter-creds-from-cfg config))
  (comment TODO polling)
  ; e.g. (twitter/get-statuses [twitter-creds username since-id 3)
  (defonce server (nrsrv/start-server :port 8999))
  (server/serve app {:open-browser? false})
  (println "Serving."))
