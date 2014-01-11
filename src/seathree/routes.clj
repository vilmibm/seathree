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

(ns seathree.routes
  (require [seathree.data   :refer :all]
           [taoensso.timbre :as log    ]))

(defn tweets 
  "Retrives tweets for front-end. They either come from redis or
   twitter's API."
  [cfg user-data]
  (log/debug "Request for " user-data)
  (let [tweets (get-tweets-from-cache cfg user-data)]
    (log/debug "got tweets:" tweets)
    (refresh-tweets! cfg user-data)
    tweets))
