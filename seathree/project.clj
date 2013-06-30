(defproject seathree "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [log4j "1.2.15" :exclusions [
                                              javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [postgresql "9.1-901.jdbc4"]
                 [ring "1.2.0-RC1"]
                 [amalloy/ring-gzip-middleware "0.1.2"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [ring/ring-json "0.2.0"]
                 [twitter-api "0.7.4"]]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler seathree.handler/app})
