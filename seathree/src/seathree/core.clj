(ns seathree.core
  (:use seathree.keys)
  (:require [clj-time.core :as time]
            [clj-time.format :as tfmt]
            [taoensso.carmine :as car :refer (wcar)]))

(def basic-formatter (tfmt/formatters :basic-date-time))
(defn time-to-str [ts] (tfmt/unparse basic-formatter ts))
(defn str-to-time [ts]
  (if (nil? ts)
    (time/minus (time/now) (time/years 1000))
    (tfmt/parse basic-formatter ts)))
(def stale (time/minutes 5))

; defaults
(def conn {:pool {} :spec {:host "localhost" :port 6379}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(defn update-last-sync 
  "Given a redis key and a timestamp, update the key with the
   timestamp"
  [key timestamp]
  (wcar* (car/set key (time-to-str timestamp))))

(defn top-off-update
  "Mark the given username's last update time as now"
  [username]
  (println "TOPPING OFF")
  (wcar* (car/set (update-key username) (time-to-str (time/now)))))

(defn update-tweets-for
  "Given a username, immediately update its update timestamp to
   indicate that we have begun the update process. Then, ask twitter for
   tweets since our since value. If we find some, translate them in
   parallel against google translate and store the resulting tweets in
   redis"
  [username since]
  (println "HELLO")
  (top-off-update username)
  ;; TODO stub
  (wcar* (car/lpush (tweets-key username) "foo" "bar" "baz" "quuz")))

(defn check-freshness
  "Given a twitter username and a timestamp, check to see if the
   user's tweets need to be refreshed"
  [username last-update]
  (future
    (if (time/before? last-update (time/minus (time/now) stale))
      (do
        (println "UPDATING TWEETS" username)
        (update-tweets-for username last-update))
      (println "NOTHING"))))

(defn tweets-for
  "Given a list of twitter handles, returns a map of username -> list
   of translated tweets"
  [usernames]
  (for [username usernames]
    (let [[tweets last-update] (wcar* (car/lrange (tweets-key username) 0 9)
                                      (car/get (update-key username)))]
      (update-last-sync (sync-key username) (time/now)) ; TODO move this to handler. Should only touch this value when an actual request comes in.
      (check-freshness username (str-to-time last-update))
      {username tweets})))
  



  
      
