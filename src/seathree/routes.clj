(ns seathree.routes
  (require [seathree.data :refer :all]))

(defn tweets 
  "Retrives tweets for front-end. They either come from redis or
   twitter's API."
  [cfg user-data]
  (let [tweets (get-tweets-from-cache cfg user-data)]
    (refresh-tweets! cfg user-data)
    tweets))
