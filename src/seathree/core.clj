(ns seathree.core
  (:use seathree.keys)
  (:require [clj-time.core    :as time             ]
            [clj-time.format  :as tfmt             ]
            [seathree.config  :as cfg              ]
            [seathree.twitter :as twitter          ]
            [taoensso.carmine :as car :refer (wcar)]))

(def basic-formatter (tfmt/formatters :basic-date-time))
(defn time-to-str [ts] (tfmt/unparse basic-formatter ts))
(defn str-to-time [ts]
  (if (nil? ts)
    (time/minus (time/now) (time/years 1000))
    (tfmt/parse basic-formatter ts)))
(def stale (time/minutes 5))

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
  (println "TOPPING OFF FOR" username)
  (wcar* (car/set (update-key username) (time-to-str (time/now)))))

(defn update-tweets-for
  "Given a username, immediately update its update timestamp to
   indicate that we have begun the update process. Then, ask twitter for
   tweets since our since value. If we find some, translate them in
   parallel against google translate and store the resulting tweets in
   redis"
  [username]
  (println "UPDATING TWEETS FOR" username)
  (top-off-update username)
  (let [creds      (twitter/creds-from-cfg (cfg/get-cfg))
        last-tweet (wcar* (car/lindex (tweets-key username) 0))
        since-id   (:id last-tweet)
        raw-tweets (twitter/get-statuses creds username since-id)]
    (comment TODO translations)
    (wcar* (apply (partial car/lpush (tweets-key username)) raw-tweets))))

(defn check-freshness
  "Given a twitter username and a timestamp, check to see if the
   user's tweets need to be refreshed"
  [username last-update]
  (future
    (if (time/before? last-update (time/minus (time/now) stale))
      (update-tweets-for username last-update)
      (println "TWEETS FRESH FOR" username))))

(defn tweets-for
  "Given a list of twitter handles, returns a map of username -> list
   of translated tweets"
  [usernames]
  (into {}
        (for [username usernames]
          (let [[tweets last-update] (wcar* (car/lrange (tweets-key username) 0 9)
                                            (car/get (update-key username)))]
            (check-freshness username (str-to-time last-update))
            [username (map :text tweets)]))))
  



  
      
