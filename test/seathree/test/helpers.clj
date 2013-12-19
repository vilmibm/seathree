(ns seathree.test.helpers)

(def _ nil)
(defn fn-lift [value] (fn [& args] value))
(def nil-fn (fn-lift nil))
(def true-fn (fn-lift true))
(def false-fn (fn-lift false))
