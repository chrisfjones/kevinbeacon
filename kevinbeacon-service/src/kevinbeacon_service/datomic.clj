(ns kevinbeacon-service.datomic
  (:use [clojure.pprint])
  (:require [clojure.string :as str]
            [datomic.api :as d]))

(def db-url "datomic:free://localhost:4334/kevinbeacon")

(def db-conn (atom nil))

(defn get-conn []
  (when (not @db-conn)
    (reset! db-conn (d/connect db-url)))
  @db-conn)
#_ (get-conn)

(defn get-db []
  (d/db (get-conn)))

(def attributes [{:db/id (d/tempid :db.part/db)
                  :db/ident :spotify/uri
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/unique :db.unique/identity
                  :db/doc "spotify uri (upsertable)"
                  :db.install/_attribute :db.part/db}
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :spotify/artist
                  :db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/one
                  :db/doc "artist for track"
                  :db.install/_attribute :db.part/db}
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :spotify/artist-name
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc "artist name"
                  :db.install/_attribute :db.part/db}
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :spotify/artist-related
                  :db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/many
                  :db/doc "related artists"
                  :db.install/_attribute :db.part/db}
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :spotify/track-name
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc "track name"
                  :db.install/_attribute :db.part/db}

                 ; user
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :user/fbid
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/unique :db.unique/identity
                  :db/doc "user fbid (upsertable)"
                  :db.install/_attribute :db.part/db}
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :user/accesstoken
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc ""
                  :db.install/_attribute :db.part/db}
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :user/first-name
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc ""
                  :db.install/_attribute :db.part/db}
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :user/last-name
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc ""
                  :db.install/_attribute :db.part/db}
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :user/gender
                  :db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/doc ""
                  :db.install/_attribute :db.part/db}

                 ; listen
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :listen/user
                  :db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/one
                  :db/doc "user who this listen is for"
                  :db.install/_attribute :db.part/db}
                 {:db/id (d/tempid :db.part/db)
                  :db/ident :listen/track
                  :db/valueType :db.type/ref
                  :db/cardinality :db.cardinality/one
                  :db/doc "track listened to"
                  :db.install/_attribute :db.part/db}
                 ])


(defn- init-db []
  (when (d/create-database db-url)
    (println "new db created"))
  (let [conn (get-conn)]
    @(d/transact conn attributes)))
#_ (init-db)

#_ (d/delete-database db-url)

#_ (let [db (get-db)]
     (->>
      (d/q '[:find ?e
             :where [?e :db/ident ?ident]
             [_ :db.install/attribute ?e]
             [(namespace ?ident) ?ns]
             [(not= ?ns "db")]
             [(not= ?ns "db.install")]
             [(not= ?ns "db.excise")]
             [(not= ?ns "fressian")]] db)
      (map first)
      (map (partial d/entity db))
      (map d/touch)
      (map :db/ident)))


; user stuff
(defn get-users []
  (let [db (get-db)]
    (some->> (d/q '[:find ?e
                    :where [?e :user/fbid]] db)
             (map first)
             (map (partial d/entity db))
             (map d/touch))))
#_ (get-users)

(defn get-user [fbid]
  (let [db (get-db)]
    (some->> (d/q '[:find ?e
                    :in $ ?fbid
                    :where [?e :user/fbid ?fbid]] db fbid)
             ffirst
             (d/entity db)
             d/touch)))
#_ (get-user "100003166650173")
#_ (get-user "100004074610459")

(defn get-all-user-ids []
  (let [db (get-db)]
    (->> (d/q '[:find ?fbid
                :in $
                :where [_ :user/fbid ?fbid]] db)
         (map first))))
#_ (get-all-user-ids)

(defn add-user [fbid accesstoken]
  (d/transact (get-conn) [{:db/id (d/tempid :db.part/user)
                           :user/fbid fbid
                           :user/accesstoken accesstoken}]))

#_ (add-user "100003166650173" "CAAIw1QRMPuUBABAMdgRbZBZAuqZA3uP61fiNXQ6Wj8McbJsU3Cnsp5aY46etcDZAkoO2KDRbsZAstNZAeuxBFxrKsJXydG1UgJDAOdzEpZCCYjHBYWHUl1i86L1nEZAatdYkzPX8RSk0o26Lx0b17poixqxkaOYWQiaaOCeWrhDggjjVHMIKk5vHnh7WgK47WWdFtF03hVfVzAZDZD")

(defn set-user-info [fbid first-name last-name gender]
  (d/transact (get-conn) [{:db/id (d/tempid :db.part/user)
                           :user/fbid fbid
                           :user/first-name first-name
                           :user/last-name last-name
                           :user/gender gender}]))



(defn add-listen [fbid uri]
  (let [user-id (d/tempid :db.part/user)
        track-id (d/tempid :db.part/user)
        listen-id (d/tempid :db.part/user)]
    (d/transact (get-conn) [{:db/id user-id
                           :user/fbid fbid}
                          {:db/id track-id
                           :spotify/uri uri}
                          {:db/id listen-id
                           :listen/user user-id
                           :listen/track track-id}])))

#_ (add-listen "100003166650173" "spotify:track:4Fzbjmsip37vodX8l3L5Pv")


(defn get-listens-for-user [fbid]
  (d/q '[:find ?l
         :in $ ?fbid
         :where [?u :user/fbid ?fbid]
         [?l :listen/user ?u]]
       (get-db) fbid))
#_ (get-listens-for-user "100003166650173")
#_ (get-listens-for-user "7812365")

(defn all-listens []
  (->>
   (d/q '[:find ?l
          :in $
          :where [?l :listen/user]]
        (get-db))
   (map first)))
#_ (count (all-listens))


; spotify stuff
(defn set-artist-for-track [track-uri {:keys [href name]}]
  (let [artist-id (d/tempid :db.part/user)
        track-id (d/tempid :db.part/user)]
    (d/transact (get-conn) [{:db/id artist-id
                             :spotify/uri href
                             :spotify/artist-name name}
                            {:db/id track-id
                             :spotify/uri track-uri
                             :spotify/artist artist-id}])))
#_ (set-artist-for-track "spotify:track:4Fzbjmsip37vodX8l3L5Pv" {:href "spotify:artist:32iIlSWFsOBxdq5BaVHL8g"
                                                                 :name "blah"})

(defn get-profile [fbid]
  (let [db (get-db)
        {first-name :user/first-name
         last-name :user/last-name
         gender :user/gender} (some->> (d/q '[:find ?u
                                              :in $ ?fbid
                                              :where [?u :user/fbid ?fbid]] db fbid)
                                       ffirst
                                       (d/entity db)
                                       d/touch)]
    {:first-name first-name
     :last-name last-name
     :gender gender
     :fbid fbid}))
#_ (get-profile "100003166650173")

(defn get-artist [uri]
  (let [db (get-db)
        {name :spotify/artist-name
         uri :spotify/uri} (some->> (d/q '[:find ?a
                                            :in $ ?artist-uri
                                            :where [?a :spotify/uri ?artist-uri]] db uri)
                                     ffirst
                                     (d/entity db)
                                     d/touch)]
    {:name name
     :uri uri}))
#_ (get-artist "spotify:artist:4Z8W4fKeB5YxbusRsdQVPb")

(defn get-artist-for-track [uri]
  (let [db (get-db)]
    (some->> (d/q '[:find ?a
                    :in $ ?track-uri
                    :where [?t :spotify/uri ?track-uri]
                    [?t :spotify/artist ?a]] db uri)
             (map first)
             (map (partial d/entity db))
             (map d/touch))))
#_ (get-artist-for-track "spotify:track:4Fzbjmsip37vodX8l3L5Pv")

(defn get-by-uri [uri]
  (let [db (get-db)]
    (some->> (d/q '[:find ?s
                    :in $ ?uri
                    :where [?s :spotify/uri ?uri]] db uri)
             ffirst
             (d/entity db)
             d/touch)))
#_ (get-by-uri "spotify:artist:4Z8W4fKeB5YxbusRsdQVPb")
#_ (get-by-uri "spotify:artist:0LVrQUinPUBFvVD5pLqmWY")


(defn artist-tx [{:keys [name uri]}]
  {:db/id (d/tempid :db.part/user)
   :spotify/uri uri
   :spotify/artist-name name})
(defn related-artist-tx [source-id id]
  [:db/add source-id :spotify/artist-related id])
(defn set-related-artists-for-artist [artist-uri related-artists]
  (let [artist-txs (map artist-tx related-artists)
        ids (apply hash-set (map :db/id artist-txs))
        source-artist-tx {:db/id (d/tempid :db.part/user)
                          :spotify/uri artist-uri}
        related-artist-txs (map (partial related-artist-tx (:db/id source-artist-tx)) ids)]
    (d/transact (get-conn) (concat artist-txs related-artist-txs [source-artist-tx]))))

#_ @(set-related-artists-for-artist "spotify:artist:4Z8W4fKeB5YxbusRsdQVPb"
                                   #{{:name "Doves", :uri "spotify:artist:0LVrQUinPUBFvVD5pLqmWY"} {:name "Thom Yorke", :uri "spotify:artist:4CvTDPKA6W06DRfBnZKrau"} {:name "Mercury Rev", :uri "spotify:artist:77oD8X9qLXZhpbCjv53l5n"} {:name "Elbow", :uri "spotify:artist:0TJB3EE2efClsYIDQ8V2Jk"} {:name "The Verve", :uri "spotify:artist:2cGwlqi3k18jFpUyTrsR84"}}
                                   )

(defn get-tracks []
  (let [db (get-db)]
    (some->> (d/q '[:find ?e
                    :where [?e :spotify/artist]] db)
             (map first)
             (map (partial d/entity db))
             (map d/touch))))
#_ (count (get-tracks))

(defn get-artists []
  (let [db (get-db)]
    (some->> (d/q '[:find ?e
                    :where [?e :spotify/artist-name]] db)
             (map first)
             (map (partial d/entity db))
             (map d/touch))))
#_ (count (get-artists))















