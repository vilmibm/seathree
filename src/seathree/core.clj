(ns seathree.core
  (:use seathree.keys)
  (:require [clj-time.core      :as time             ]
            [clj-time.format    :as tfmt             ]
            [seathree.config    :as cfg              ]
            [seathree.translate :as translate        ]
            [seathree.twitter   :as twitter          ]
            [taoensso.carmine   :as car :refer (wcar)]))

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
  [person timestamp]
  (println "UPDATING SYNC FOR" person)
  (wcar* (car/set (sync-key person) (time-to-str timestamp))))

(defn top-off-update
  "Mark the given person's last update time as now"
  [person]
  (println "TOPPING OFF FOR" person)
  (wcar* (car/set (update-key person) (time-to-str (time/now)))))

(defn update-tweets-for
  "Given a person, immediately update its update timestamp to
   indicate that we have begun the update process. Then, ask twitter for
   tweets since our since value. If we find some, translate them in
   parallel against google translate and store the resulting tweets in
   redis"
  [person]
  (println "UPDATING TWEETS FOR" person)
  (top-off-update person)
  (let [creds      (twitter/creds-from-cfg (cfg/get-cfg))
        last-tweet (wcar* (car/lindex (tweets-key person) 0))
        since-id   (:id last-tweet)
        raw-tweets (twitter/get-statuses creds (:username person) since-id)
        translated (for [raw-tweet raw-tweets]
                     (translate/translate raw-tweet (:src person) (:tgt person) 3))]
    (wcar* (apply (partial car/lpush (tweets-key person)) translated))))

(defn check-freshness
  "Given a person and a timestamp, check to see if the
   user's tweets need to be refreshed"
  [person last-update]
  (if (time/before? last-update (time/minus (time/now) stale))
    (update-tweets-for person last-update)
    (println "TWEETS FRESH FOR" person)))

(defn tweets-for
  "Given a list of person maps, returns a list of maps of people with translated tweets"
  [people]
  (for [person people]
    (let [[tweets last-update] (wcar* (car/lrange (tweets-key person) 0 9)
                                      (car/get (update-key person)))]
      (check-freshness person (str-to-time last-update))
      (assoc person :tweets tweets))))
