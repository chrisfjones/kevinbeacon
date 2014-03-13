(ns kevinbeacon-service.echonest
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(let [artist-id "4OztY2IdZMlLvdlBwJnOkM"
      url (str "http://developer.echonest.com/api/v4/artist/similar?api_key=7NJIFKRJFILS82ZK8&id=spotify-WW:artist:" artist-id "&format=json&results=5&start=0")
      {:keys [status headers body error] :as resp} @(http/get url)]
    (json/read-str body :key-fn keyword))



