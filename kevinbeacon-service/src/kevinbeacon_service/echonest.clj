(ns kevinbeacon-service.echonest
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(defn fix-uri [uri]
  (let [[_ _ artist-id] (str/split uri #":")]
    (str "spotify:artist:" artist-id)))

(defn slim-artist [{:keys [name foreign_ids]}]
  {:name name
   :uri (fix-uri (:foreign_id (first foreign_ids)))})

(defn grab-similar-artists [artist-uri]
  (let [[_ _ artist-id] (str/split artist-uri #":")
        url (str "http://developer.echonest.com/api/v4/artist/similar?api_key=7NJIFKRJFILS82ZK8&bucket=id:spotify-WW&id=spotify-WW:artist:" artist-id "&format=json&results=5&start=0")
        {:keys [status headers body error] :as resp} @(http/get url)]
    (->> (json/read-str body :key-fn keyword)
         :response
         :artists
         (map slim-artist)
         (apply hash-set))))
#_ (grab-similar-artists "spotify:artist:4Z8W4fKeB5YxbusRsdQVPb")



