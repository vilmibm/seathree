(ns seathree.keys
  (:use [clojure.string :only [join]]))

(defn gen-key
  "Generates redis keys based on the form <lookup>_<name>"
  [name person]
  (join "_" [(:username person) name]))

(def tweets-key (partial gen-key "tweets"))
(def sync-key   (partial gen-key "sync"))
(def update-key (partial gen-key "update"))
