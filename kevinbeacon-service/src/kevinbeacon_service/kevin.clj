(ns kevinbeacon-service.kevin
  (:require [datomic.api :as d]
            [kevinbeacon-service.datomic :as db]))

(def users (atom []))


(defn grouper [thing]
  thing)

(defn top-artists-for-user [fbid]
  (let [db (db/get-db)
        artist-tracks (d/q '[:find ?artist-uri ?track-uri
                             :in $ ?fbid
                             :where [?u :user/fbid ?fbid]
                             [?l :listen/user ?u]
                             [?l :listen/track ?t]
                             [?t :spotify/uri ?track-uri]
                             [?t :spotify/artist ?a]
                             [?a :spotify/uri ?artist-uri]] (db/get-db) fbid)]
    (->> artist-tracks
         (map first)
         frequencies
         (sort-by second)
         reverse
         (map first))))

(defn top-artists-for-all []
  (let [db (db/get-db)
        artist-tracks (d/q '[:find ?artist-uri ?track-uri
                             :where [?l :listen/track ?t]
                             [?t :spotify/uri ?track-uri]
                             [?t :spotify/artist ?a]
                             [?a :spotify/uri ?artist-uri]] (db/get-db))]
    (->> artist-tracks
         (map first)
         frequencies
         (sort-by second)
         reverse
         (map first))))
#_ (top-artists-for-all)

(defn random-track-for-artist [artist-uri]
  (let [db (db/get-db)]
    (->> (d/q '[:find (rand ?track-uri)
                :in $ ?artist-uri
                :where [?a :spotify/uri ?artist-uri]
                [?t :spotify/artist ?a]
                [?t :spotify/uri ?track-uri]] (db/get-db) artist-uri)
         ffirst)))
#_ (random-track-for-artist "spotify:artist:7tA9Eeeb68kkiG9Nrvuzmi")

; this is the big one
(defn find-connection [])

 (top-artists-for-user "100003166650173")
