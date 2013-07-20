(ns seathree.translate
  (use clojure.java.shell))

(defn failed? [result] (> (:exit result) 0))

(defn translate
  "Given some string, pass it off to the Python script that interacts
   with the Python API. Note that this is blocking and Google often takes
   a long time to respond. You probably want to invoke this with future."
  [text src tgt retries]
  (if (> retries 0)
    (let [result (sh "python" "resources/python/translate.py" src tgt (format "'%s'" text))]
      (if (failed? result)
        (translate text (- retries 1))
        (:out result)))))
    
