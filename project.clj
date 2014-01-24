(defproject seathree "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [clj-time "0.5.1"]
                 [clj-http "0.7.8"]
                 [com.taoensso/carmine "2.0.0-beta2"]
                 [ring "1.2.0-RC1"]
                 [amalloy/ring-gzip-middleware "0.1.2"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [ring/ring-json "0.2.0"]
                 [ring-cors "0.1.0"]
                 [ring-server "0.2.8"]
                 [twitter-api "0.7.5"]]
  :plugins [[lein-ring "0.8.5"]]
  :main seathree.handler
  :ring {:handler seathree.handler/app})
