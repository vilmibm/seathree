(ns seathree.twitter
  (use twitter.oauth
       twitter.api.restful)
  (require [seathree.config :as cfg]))

(def creds (let [twitter-creds (cfg/get-twitter-creds)]
             (make-oauth-creds (:consumer-key        twitter-creds)
                               (:consuer-secret      twitter-creds)
                               (:access-token        twitter-creds)
                               (:access-token-secret twitter-creds))))

(defn match [re s] (not (nil? (re-seq re s))))

(defn extract-tweets-from-body
  "TODO"
  [body]
  (map :text body))
  

(defn get-statuses
  "TODO"
  [username since-id retries]
  (if (= 0 retries)
    nil
    (let [base-params   {:screen-name username :include-rts false}
          params        (if since-id (assoc base-params :since-id since-id) base-params)
          response      (statuses-user-timeline :oauth-creds creds :params params)
          status-string (format "%d" (:code (:status response)))] 
      (condp match status-string
        #"[45].." (do
                    (println "Twitter threw a" status-string "for" username ":" (:msg (:status response)))
                    (get-statuses username since-id (- retries 1)))
        #"2.."    (extract-tweets-from-body (:body response))))))
