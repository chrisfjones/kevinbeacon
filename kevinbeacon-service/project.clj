(defproject kevinbeacon-service "0.0.1-SNAPSHOT"
  :dependencies [; clojure
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.278.0-76b25b-alpha"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/core.cache "0.6.3"]
                 [org.clojure/core.memoize "0.5.6" :exclusions [org.clojure/core.cache]]

                 [io.pedestal/pedestal.service "0.2.2"]
                 [io.pedestal/pedestal.service-tools "0.2.2"]
                 [io.pedestal/pedestal.jetty "0.2.2"]
                 ;; [io.pedestal/pedestal.tomcat "0.2.2"]

                 [org.clojure/tools.logging "0.2.4"]
                 [ch.qos.logback/logback-classic "1.0.7"]

                 ; datomic
                 [com.datomic/datomic-pro "0.8.4143"
                  :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]

                 [org.apache.httpcomponents/httpclient "4.3.2"]
                 [clj-http "0.7.0" :exclusions [crouton]]
                 [http-kit "2.1.13"]
                 ]
  :jvm-opts ["-Dfile.encoding=utf-8" "-Xms512m" "-Xmx1g"]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]
  :aliases {"run-dev" ["trampoline" "run" "-m" "kevinbeacon-service.server/run-dev"]}
  :repl-options  {:init-ns user
                  :init (try
                          (use 'io.pedestal.service-tools.dev)
                          (require 'kevinbeacon-service.service)
                          ;; Nasty trick to get around being unable to reference non-clojure.core symbols in :init
                          (eval '(init kevinbeacon-service.service/service #'kevinbeacon-service.service/routes))
                          (catch Throwable t
                            (println "ERROR: There was a problem loading io.pedestal.service-tools.dev")
                            (clojure.stacktrace/print-stack-trace t)
                            (println)))
                  :welcome (println "Welcome to pedestal-service! Run (tools-help) to see a list of useful functions.")}
  :main ^{:skip-aot true} kevinbeacon-service.server)
