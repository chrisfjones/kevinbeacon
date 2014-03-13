(ns kevinbeacon-service.handlers
  (:require [clojure.core.async :as async :refer [<! >! <!! >!! timeout chan buffer alt! alts! go go-loop close! pipe]]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [ring.util.response :as response]))


(defn handle-register [{:keys [session json-params] :as req}]
  (let [new-session (assoc session :fbid (get json-params :fbid))]
    (->> {:status "ok"}
       response/response
       (#(assoc % :session new-session)))))

