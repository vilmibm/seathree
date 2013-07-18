(ns seathree.config)

(comment TODO don't hardcode this to something in the source)
(def config-path "resources/secrets.clj")

(defn get-cfg
  "TODO"
  []
  (eval (read-string (slurp config-path))))
