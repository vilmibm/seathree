(ns seathree.keys
  (:use [clojure.string]))

(defn gen-key
  "Generates redis keys based on the form <lookup>_<name>"
  [name lookup]
  (join "_" [lookup name]))

(def tweets-key (partial gen-key "tweets"))
(def sync-key   (partial gen-key "sync"))
(def update-key (partial gen-key "update"))
