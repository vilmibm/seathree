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

(ns seathree.data
  (require [clojure.string       :as str              ]
           [cheshire.core        :as json             ]
           [clj-http.client      :as http-client      ]
           [clj-time.core        :as time             ]
           [clj-time.format      :as tfmt             ]
           [taoensso.carmine     :as car :refer (wcar)]
           [taoensso.timbre      :as log              ]
           [twitter.oauth        :as oauth            ]
           [twitter.api.restful  :as twitter          ]))

(def stale (time/minutes 5))
(def translate-url "https://www.googleapis.com/language/translate/v2")

(defn to-string [int] (format "%d" int))

(defn redis
  "Function-based wrapper around redis API. A macro would be nicer but
   I'm not sure how to mock out macros for tests."
  [cfg function]
  (log/debug "calling redis")
  (wcar {:pool {} :spec (select-keys (:redis cfg) [:host :port])}
        (function)))

(defmacro redis'
  "useful when testing in REPL"
  [cfg & body]
  `(wcar {:pool {} :spec (select-keys (:redis ~cfg) [:host :port])}
         ~@body))

(defn process-failed? [result] (> (:exit result) 0))

(defn match [re s] (not (nil? (re-seq re s))))

;; Keys for storing data in Redis. Transformations on user-data maps.

(defn gen-key'
  "Generates redis key based on a map of user data plus the type of key"
  [key-name user-data]
  (str/join "_" [(name key-name) (:username user-data) (:src user-data) (:tgt user-data)]))

(def gen-key (memoize gen-key'))
(def refresh-key (partial gen-key :refresh))
(def tweets-key  (partial gen-key :tweets))

(def formatter (tfmt/formatters :basic-date-time))

(defn store-ts!
  "For the given key function, user-data, and optional timestamp,
   store the timestamp in Redis. If no timestamp is provided, time/now
   is used."
  [cfg key-fn user-data & [timestamp]]
  (let [key       (key-fn user-data)
        timestamp (or timestamp (time/now))]
    (redis cfg #(car/set key (tfmt/unparse formatter timestamp)))))

(defn get-ts
  "For the given key function and user-data, return the stored
   timestamp from Redis."
  [cfg key-fn user-data]
  (let [key       (key-fn user-data)
        ts-string (redis cfg #(car/get key))]
    (if (nil? ts-string)
      (time/minus (time/now) (time/years 1000))
      (tfmt/parse formatter ts-string))))

(defn with-retries [retries function]
  (if (> retries 0)
    (loop [retries-left retries]
      (let [result (function)]
        (if (nil? result)
          (if (> retries-left 0) (recur (dec retries-left)) nil)
          result)))))

(defn extract-translation
  "Given a 200 response from the google translate API, pull out the
   first translation."
  [response]
  (let [data (json/parse-string (:body response) true)]
    (:translatedText (first (:translations (:data data))))))

(defn mk-sigil
    "
    Create a sigil used to replace Twitter symbols like
    @replies. Necessary to preserve symbols across trips to
    google. Also cuts down on google character usage. Got to save
    dollars.

    Takes an int, returns a string like `xz0`.
    "
    [c]
    (format "XZ%d" c))
  
(defn mark-sigils
    "Replace special forms within a tweet with ordered sigils so
    they can be restored later. For example,

    Este cerveza esta sabrosa #cerveza http://bit.ly/foo @friend

    will be transformed into

    Este cerveza esta sabrosa XZ1 XZ2 XZ3

    (these will be later reconstructed).

    Returns a tuple of a marked string and a listing of symbols that
    were replaced with sigils."
    [raw-text]
    (let [symbol-re #"\@\w+|http:\/\/[^ ]+|\#\w+"
          symbols   (re-seq symbol-re raw-text)]
      (loop [c           0
             marked-text raw-text
             ss          symbols]
        (if (empty? ss)
          [marked-text symbols]
          (recur (inc c)
                 (str/replace marked-text (first ss) (mk-sigil c))
                 (rest ss))))))

(defn restore-sigils
    "
    Restore a list of symbols to their placeholders. Accepts the
    output of `mark-sigils`.
    "
    [marked-text symbols]
    (loop [c             0
           restored-text marked-text
           ss            symbols]
      (if (empty? ss)
        restored-text
        (recur (inc c)
               (str/replace restored-text (mk-sigil c) (first ss))
               (rest ss)))))

(defn translate
  "Given a user-data map and a single tweet's text, make a GET request
   to the google translate API."
  [cfg user-data tweet]
  (let [key                   (:key (:google cfg))
        src                   (:src user-data)
        tgt                   (:tgt user-data)
        [marked-text symbols] (mark-sigils (:text tweet))
        http-opts             {:query-params {"key" key "source" src "target" tgt "q" marked-text}}
        result                (http-client/get translate-url http-opts)
        status-string         (to-string (:status result))]
    (condp match status-string
      #"^[45]"  nil
      #"^2"     (assoc tweet :translated (-> result
                                             extract-translation
                                             (restore-sigils symbols)))
      #"null"  nil)))

(defn twitter-creds-from-cfg
  "Given a seathree config map, produce twitter oauth credentials."
  [cfg]
  (let [creds (:oauth (:twitter cfg))]
    (apply oauth/make-oauth-creds (map #(% creds) [:consumer-key :consumer-secret
                                                   :access-token :access-token-secret]))))


(defn extract-tweet
  "Pulls only the keys we care about from a tweet from twitter"
  [tweet]
  (let [username    (:screen_name (:user tweet))
        displayname (:name (:user tweet))
        tweet       (select-keys tweet [:text :id :created_at])]
    (assoc tweet :username username)
    (assoc tweet :displayname displayname)))
 
(defn get-tweets-from-twitter
  "Ask the Twitter API for tweets for the given user-data map. Fetches
   up until last-tweet-id if present; if nil, fetches last 20 tweets."
  [cfg user-data & [last-tweet-id]]
  (log/debug "Calling Twitter API for" user-data last-tweet-id)
  (let [creds         (twitter-creds-from-cfg cfg)
        basic-params  {:screen-name (:username user-data) :include-rts false}
        params        (if last-tweet-id (assoc basic-params :since-id last-tweet-id) basic-params)
        response      (twitter/statuses-user-timeline :oauth-creds creds :params params)
        status-string (to-string (:code (:status response)))]
    (condp match status-string
      #"^[45]" nil
      #"^2"    (map extract-tweet (:body response)))))

(defn get-tweets-from-cache
  "Pull out and return all tweet data for the requested user
   map. Assocs tweet list with user data."
  [cfg user-data & [since-id]]
  (let [tweets (redis cfg #(car/lrange (tweets-key user-data) 0 -1))]
    (if since-id
      (take-while #(not (= (:id %) since-id)) tweets)
      tweets)))

(defn store-tweets [cfg user-data tweets]
  (let [tkey    (tweets-key user-data)
        last-id (:id (redis cfg #(car/lindex tkey 0)))
        tweets  (if last-id (filter #(< last-id (:id %)) tweets) tweets)]
    (redis cfg #(apply (partial car/lpush tkey) (reverse tweets)))))
    
(defn refresh-tweets!
  "Actually ask for new tweets from twitter for the given user map. If
   we see new tweets, translate and store them. Spawns a thread."
  [cfg user-data]
  (log/debug "Spawning refresh thread for " user-data)
  ;; Spawn a thread to do this work since it involves Redis, Twitter
  ;; and Google.
  (future
    (log/debug "Refreshing... " user-data)
    ;; First, get the timestamp for the last time these tweets were
    ;; refreshed. We might not have to do any work.
    (let [last-refresh (get-ts cfg refresh-key user-data)]
      (if (time/before? last-refresh (time/minus (time/now) stale))
        (do
          (log/debug "Tweets are stale for" user-data)
          ;; Second, update the last refresh timestamp for the given
          ;; user data map. That way, if another thread attempts to run
          ;; this update it will stop and not duplicate effort.
          (store-ts! cfg refresh-key user-data)
          ;; Now for actually refreshing tweets. Start by getting the
          ;; most recent tweet for this person (We only want to ask for
          ;; tweets after this tweet's id)
          (let [last-tweet-id     (:id (redis cfg #(car/lindex (tweets-key user-data) 0)))
                tweets            (with-retries 3 #(get-tweets-from-twitter cfg user-data last-tweet-id))]
            (if (not (nil? tweets))
              (let [retrying-translate (fn [text] (with-retries 3 #(translate cfg user-data text)))
                    translated-tweets  (->> tweets
                                            (map retrying-translate)
                                            (filter #(not (nil? %))))]
                (if (not (empty? translated-tweets))
                  (store-tweets cfg user-data translated-tweets)
                  (log/debug "Failed to translate any tweets"))))))))))
