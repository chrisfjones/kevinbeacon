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

(defn add-user [fbid accesstoken]
  (d/transact (get-conn) [{:db/id (d/tempid :db.part/user)
                           :user/fbid fbid
                           :user/accesstoken accesstoken}]))

#_ (add-user "100003166650173" "CAAIw1QRMPuUBABAMdgRbZBZAuqZA3uP61fiNXQ6Wj8McbJsU3Cnsp5aY46etcDZAkoO2KDRbsZAstNZAeuxBFxrKsJXydG1UgJDAOdzEpZCCYjHBYWHUl1i86L1nEZAatdYkzPX8RSk0o26Lx0b17poixqxkaOYWQiaaOCeWrhDggjjVHMIKk5vHnh7WgK47WWdFtF03hVfVzAZDZD")








; spotify stuff
(defn set-artist-for-track [track-uri artist-uri]
  (let [artist-id (d/tempid :db.part/user)
        track-id (d/tempid :db.part/user)]
    (d/transact (get-conn) [{:db/id artist-id
                             :spotify/uri artist-uri}
                            {:db/id track-id
                             :spotify/uri track-uri
                             :spotify/artist artist-id}])))
#_ (set-artist-for-track "spotify:track:4Fzbjmsip37vodX8l3L5Pv" "spotify:artist:32iIlSWFsOBxdq5BaVHL8g")


(defn get-tracks []
  (let [db (get-db)]
    (some->> (d/q '[:find ?e
                    :where [?e :spotify/artist]] db)
             (map first)
             (map (partial d/entity db))
             (map d/touch))))
#_ (get-tracks)














