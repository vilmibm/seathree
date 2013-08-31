(ns seathree.translate
  (use clojure.java.shell
       seathree.config))

(defn failed? [result] (> (:exit result) 0))

(defn translate
  "Given some string, pass it off to the Python script that interacts
   with the Python API. Note that this is blocking and Google often takes
   a long time to respond. You probably want to invoke this with future."
  [text src tgt retries]
  (if (> retries 0)
    (let [key    (:key (:google (get-cfg))) ; TODO reading file every time
          result (sh "python" "resources/python/translate.py" key src tgt (format "'%s'" text))]
      (if (failed? result)
        (translate text (- retries 1))
        (clojure.string/trim-newline (:out result))))))
    
