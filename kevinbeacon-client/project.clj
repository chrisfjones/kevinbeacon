(defproject kevinbeacon-client "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [cljs-ajax "0.2.3"]
                 [prismatic/dommy "0.1.2"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "kevinbeacon-client"
              :source-paths ["src"]
              :compiler {
                :output-to "kevinbeacon_client.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
