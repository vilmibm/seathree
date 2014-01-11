; SeaThree, Realtime Twitter Translations
; Copyright (C) 2014 Nathaniel Smith and Benjamin Valentine
;
; This program is free software: you can redistribute it and/or modify
; it under the terms of the GNU Affero General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; This program is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU Affero General Public License for more details.
;
; You should have received a copy of the GNU Affero General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns seathree.test.config
  (:require [clojure.test :refer :all]
            [seathree.config :as cfg]))

(def config {:hi "there" :how {:are "you"}})

(deftest evals-form
  (let [evaled-config (with-redefs [slurp (fn [_] "{:hi \"there\" :how {:are \"you\"}}")]
                        (cfg/get-cfg "foo"))]
    (is (= config evaled-config)))) 

(deftest foo
  (is (= 1 1)))
