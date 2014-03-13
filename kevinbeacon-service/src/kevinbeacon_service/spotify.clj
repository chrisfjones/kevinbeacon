(ns kevinbeacon-service.spotify
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(defn spload [uri]
  (let [url (str "http://ws.spotify.com/lookup/1/.json?uri=" uri)
        {:keys [status headers body error] :as resp} @(http/get url)]
    (json/read-str body :key-fn keyword)))
#_ (spload "spotify:track:5ftKi1XWbwGv5J6ZZ4WjPF")

(defn load-artist-for-track [track-uri]
  (->> (spload track-uri)
       :track
       :artists
       first))
#_ (load-artist-for-track "spotify:track:5ftKi1XWbwGv5J6ZZ4WjPF")
