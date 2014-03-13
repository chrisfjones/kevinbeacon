(ns kevinbeacon-service.facebook
  (:require [clojure.string :as str]
            [clj-http.client :as clj-http]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]
            [kevinbeacon-service.datomic :as db]))

(defn- fb-query-opts [limit token]
  {:query-params {:fields (str "first_name,last_name,gender,music.listens.fields(data).app_id_filter(174829003346).limit(" limit ")")
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
        opts (fb-query-opts 200 access-token)
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
        gender (when data
                 (-> data
                     (get "gender")))
        last-name (when data
                    (-> data
                        (get "last_name")))]
    (db/set-user-info fbid first-name last-name gender)
    (->> listens
         (map process-raw-listen)
         (map :music.song))))

#_ (load-listens
    "100003166650173"
    "CAAIw1QRMPuUBANt61NiD3GZCXlb4CCaKb0lQrGh7A3w8G8lK7efJAeZBK8kva20n48CoL6k5QZA8oOVOPpM9W99h8C2v5vraVJceUEHpb9ZBx4kuvtokp2fDDlS96nLfuVUtD52EUuP1rZCQbbjaJZBQdJ0525pONX1HE1sqb3SDTkgI49Mk5fXen0ZA6tWPcsZD"
    )

