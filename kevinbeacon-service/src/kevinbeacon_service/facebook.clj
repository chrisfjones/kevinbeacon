(ns kevinbeacon-service.facebook
  (:require [clojure.string :as str]
            [clj-http.client :as clj-http]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(defn- fb-query-opts [limit token]
  {:query-params {:fields (str "first_name,last_name,music.listens.fields(data).app_id_filter(174829003346).limit(" limit ")")
                  :access_token token}
   :insecure? true
   :follow-redirects true
   :keepalive -1})

(defn- spotify-uri-from-url [url]
  (let [[_ _ _ type id] (str/split url #"/")]
    (str/join ":" ["spotify" type id])))

(defn- process-listen-type [{:strs [url type] :as listen-type}]
  (try
    [(keyword type) (spotify-uri-from-url url)]
    (catch Exception e
      (println "issue with listen-type" listen-type e ", moving on"))))

(defn process-raw-listen [raw-listen]
  (->> (get raw-listen "data")
       vals
       (map process-listen-type)
       (filter (comp not nil?))
       flatten
       (apply hash-map)))

#_ (process-raw-listen {"data"
                     {"song"
                      {"id" "10150319325873043",
                       "url" "http://open.spotify.com/track/52qSJax3e7a90wuu007hTa",
                       "type" "music.song",
                       "title" "Ocean Waves Crashing for Wellness And Well Being"}},
                     "id" "543216502460567"})

(defn load-listens [fbid access-token]
  (let [fb-base-url (str "https://graph.facebook.com/" fbid)
        opts (fb-query-opts 100 access-token)
        {:keys [status headers body error] :as resp} (clj-http/get fb-base-url opts)
        data (try
               (json/read-str body)
               (catch Exception e
                 (println "status:" status "error:" error)
                 (println "body:" body)))
        listens (when data
                  (-> data
                      (get "music.listens")
                      (get "data")))
        first-name (when data
                     (-> data
                         (get "first_name")))
        last-name (when data
                    (-> data
                        (get "last_name")))]
    (->> listens
         (map process-raw-listen)
         (map :music.song))))

#_ (load-listens
    "100003166650173"
    "CAAIw1QRMPuUBABAMdgRbZBZAuqZA3uP61fiNXQ6Wj8McbJsU3Cnsp5aY46etcDZAkoO2KDRbsZAstNZAeuxBFxrKsJXydG1UgJDAOdzEpZCCYjHBYWHUl1i86L1nEZAatdYkzPX8RSk0o26Lx0b17poixqxkaOYWQiaaOCeWrhDggjjVHMIKk5vHnh7WgK47WWdFtF03hVfVzAZDZD"
    )

