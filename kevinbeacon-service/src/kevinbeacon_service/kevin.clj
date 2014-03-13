(ns kevinbeacon-service.kevin
  (:require [datomic.api :as d]
            [kevinbeacon-service.datomic :as db]))

(def messages (atom {}))

(defn consume-message-for-user [fbid]
  ; not thread safe :(
  (let [result (first (get-in @messages [fbid]))]
    (swap! messages update-in [fbid] rest)
    result))
#_ (consume-message-for-user "100003166650173")

(defn add-message-for-user [fbid msg]
  (swap! messages update-in [fbid] #(conj % msg)))
#_ (add-message-for-user "100003166650173" {:yo "hey"})
#_ (add-message-for-user "100004074610459" {:yo "guy"})

(defn message-all-users [msg]
  (doseq [fbid (db/get-all-user-ids)]
    (add-message-for-user fbid msg)))

(defn related-artists-for-artist [artist-uri]
  (let [db (db/get-db)]
    (->> (d/q '[:find ?related
                :in $ ?artist-uri
                :where [?a :spotify/uri ?artist-uri]
                [?a :spotify/artist-related ?related]] db artist-uri)
         (map first)
         (map (partial d/entity db))
         (map :spotify/uri))))
#_ (related-artists-for-artist "spotify:artist:7tA9Eeeb68kkiG9Nrvuzmi")

(defn message-track-lovers [track-uri]
  (let [db (db/get-db)
        artist-uri (->> (d/q '[:find ?artist-uri
                               :in $ ?track-uri
                               :where [?t :spotify/uri ?track-uri]
                               [?t :spotify/artist ?a]
                               [?a :spotify/uri ?artist-uri]] db track-uri)
                        ffirst)
        lovers (atom [])]
    (doseq [fbid (db/get-all-user-ids)]
      (let [user-artist-pool (apply hash-set (top-artists-for-user fbid))
            profile (db/get-profile fbid)]
        (if (get user-artist-pool artist-uri)
          ; user likes this artist
          (do
            (message-all-users {:loves (db/get-artist artist-uri) :profile profile})
            (swap! lovers conj fbid))
          (doseq [artist user-artist-pool]
            (let [related-pool (related-artists-for-artist artist)]
              (when (get related-pool artist-uri)
                ; user likes a related artist
                (do
                  (message-all-users {:related (db/get-artist artist) :profile profile})
                  (swap! lovers conj fbid))))))))
    ; handle lovers
    (when (> (count @lovers) 1)
      (let [l1 (first @lovers)
            l2 (second @lovers)]
        (add-message-for-user l1 {:hookup (db/get-artist artist-uri)
                                  :profile (db/get-profile l2)})
        (add-message-for-user l2 {:hookup (db/get-artist artist-uri)
                                  :profile (db/get-profile l1)})
        ))))

{:hookup {:uri "spotify:artist:1WvvwcQx0tj6NdDhZZ2zZz"
                             :name ""
                             :artist "future islands"}
                    :profile {:fbid "7812365"
                              :first-name "Cathleen"
                              :gender "female"}}

#_ (message-track-lovers "spotify:track:2dLLR6qlu5UJ5gk0dKz0h3")

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
         ;(filter #(> (second %) 1))
         (sort-by second)
         reverse
         (map first))))
#_ (top-artists-for-user "100003166650173")
#_ (top-artists-for-user "7812365")

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
         ;(filter #(> (second %) 1))
         (sort-by second)
         reverse
         (map first))))
#_ (top-artists-for-all)

(defn track-from-uri [uri]
  (let [db (db/get-db)
        track-raw (->> (d/q '[:find ?artist-name
                              :in $ ?uri
                              :where [?t :spotify/uri ?uri]
                              ;[?t :spotify/track-name ?track-name]
                              [?t :spotify/artist ?a]
                              [?a :spotify/artist-name ?artist-name]] (db/get-db) uri)
                       first)]
    {:uri uri
     :artist (first track-raw)
     :name (second track-raw)}))
#_ (track-from-uri "spotify:track:2dLLR6qlu5UJ5gk0dKz0h3")

; todo: find tracks that more than one user likes
(defn random-track-for-artist [artist-uri]
  (let [db (db/get-db)
        track-uri (->> (d/q '[:find (rand ?track-uri)
                              :in $ ?artist-uri
                              :where [?a :spotify/uri ?artist-uri]
                              [?t :spotify/artist ?a]
                              [?t :spotify/uri ?track-uri]] (db/get-db) artist-uri)
                       ffirst)
        track (track-from-uri track-uri)]
    (message-all-users {:track track})
    (future
      (Thread/sleep 1000)
      (message-track-lovers track-uri))
    track-uri))
#_ (random-track-for-artist "spotify:artist:7tA9Eeeb68kkiG9Nrvuzmi")

