(ns seathree.config)

(comment TODO don't hardcode this to something in the source)
(def config-path "resources/secrets.clj")

(defn get-cfg
  "TODO"
  [path]
  (eval (read-string (slurp path))))

(defn get-twitter-creds
  "TODO"
  []
  (:oauth (:twitter (get-cfg config-path))))

 
