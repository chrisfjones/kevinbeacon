(ns kevinbeacon-service.handlers
  (:require [clojure.core.async :as async :refer [<! >! <!! >!! timeout chan buffer alt! alts! go go-loop close! pipe]]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [kevinbeacon-service.datomic :as db]
            [kevinbeacon-service.facebook :as fb]
            [kevinbeacon-service.spotify :as sp]
            [kevinbeacon-service.echonest :as ec]
            [kevinbeacon-service.kevin :as k]
            [ring.util.response :as response]))

(defn save-artist-if-blank [track-uri]
  (let [existing-artist (db/get-artist-for-track track-uri)]
    (when (empty? existing-artist)
      (let [{:keys [name href] :as artist} (sp/load-artist-for-track track-uri)]
        (db/set-artist-for-track track-uri artist)
        (db/set-related-artists-for-artist href (ec/grab-similar-artists href))))))

(defn handle-register [{:keys [session json-params] :as req}]
  (let [fbid (get json-params :fbid)
        token (get json-params :accesstoken)
        new-session (assoc session :fbid fbid)
        existing-user (db/get-user fbid)]
    (when (not existing-user)
      (db/add-user fbid token)
      (future
        (let [listens (fb/load-listens fbid token)]
          (doall (map (partial db/add-listen fbid) listens))
          (doall (map save-artist-if-blank listens)))))
    (->> {:status "ok"}
       response/response
       (#(assoc % :session new-session)))))

(defn handle-next-track [req]
  (->> (k/top-artists-for-all)
       (take 5)
       shuffle
       first
       k/random-track-for-artist
       (assoc {} :uri)
       response/response))
