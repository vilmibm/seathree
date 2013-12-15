(ns seathree.config)


(defn get-cfg
  "Slurp & eval a clojure file that defines a map containing configuration
   information."
  [cfg-path]
  (eval (read-string (slurp cfg-path))))
