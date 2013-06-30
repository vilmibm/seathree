(ns seathree.handler
  (use clojure.tools.logging
       ring.middleware.json
       seathree.core)
  (require [clojure.data.json :as json]))

; TODO gzipping

(defn valid?
  "TODO"
  [request]
  (let [content-type ("content-type" (:headers request))
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
  (println request)
  (if (valid? request)
    (success (extract-usernames request))
    (fail)))

(def app
  (wrap-json-body handler {:keywords? true}))
