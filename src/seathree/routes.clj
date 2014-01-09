(ns seathree.routes
  (require [seathree.data   :refer :all]
           [taoensso.timbre :as log    ]))

(defn tweets 
  "Retrives tweets for front-end. They either come from redis or
   twitter's API."
  [cfg user-data]
  (log/debug "Request for " user-data)
  (let [tweets (get-tweets-from-cache cfg user-data)]
    (log/debug "got tweets:" tweets)
    (refresh-tweets! cfg user-data)
    tweets))
