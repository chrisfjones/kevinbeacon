(ns kevinbeacon-client.core
  (:require
   [dommy.core :as dommy]
   [dommy.attrs :as attrs]
   [clojure.string :as str]
   [ajax.core :as ajax]
   [cljs.core.async :refer [alts! put! chan <! >! timeout] :as async])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:use-macros
   [dommy.macros :only [node sel sel1 deftemplate]]))

(enable-console-print!)

(defn render-track [{:keys [uri name artist]}]
  (dommy/clear! (sel1 :#messages))
  (attrs/set-attr! (sel1 :#track-image)
                   :src (str "http://resonatewith.me/api/spotify/images/" uri ".jpg"))
  (dommy/set-text! (sel1 :#track-name) name)
  (dommy/set-text! (sel1 :#artist-name) artist))

(defn add-love [{:keys [fbid first-name]} {:keys [name uri]}]
  (dommy/append! (sel1 :#messages) (node
                                    [:li
                                     [:.profile-pic
                                      [:img {:src (str "http://graph.facebook.com/" fbid "/picture")}]]
                                     [:.love-text (str first-name " loves '" name "'")]])))
#_ (add-love {:fbid "100003166650173" :first-name "Chris"}
             {:name "Future Islands" :uri "spotify:artist:1WvvwcQx0tj6NdDhZZ2zZz"})

(defn add-related [{:keys [fbid first-name]} {:keys [name uri]}]
  (dommy/append! (sel1 :#messages) (node
                                    [:li
                                     [:.profile-pic.related
                                      [:img {:src (str "http://graph.facebook.com/" fbid "/picture")}]]
                                     [:.related-text (str first-name " likes a related artist '" name "'")]])))
#_ (add-related {:fbid "100003166650173" :first-name "Chris"}
                {:name "Future Islands" :uri "spotify:artist:1WvvwcQx0tj6NdDhZZ2zZz"})

(defn add-hookup [{:keys [fbid first-name gender]} {:keys [name uri]}]
  (let [gen-text (if (= "male" gender)
                   "him!"
                   "her!")]
    (dommy/append! (sel1 :#messages) (node
                                    [:li.hookup-wrapper
                                     [:.profile-pic.related
                                      [:img {:src (str "http://graph.facebook.com/" fbid "/picture")}]
                                      [:span "+"]
                                      [:img {:src (str "http://graph.facebook.com/" my-fbid "/picture")}]
                                      [:span "="]
                                      [:img {:src (str "http://resonatewith.me/api/spotify/images/" uri ".jpg")}]]
                                     [:h3.hookup-text (str "go talk to " gen-text)]]))))

(defn handle-message [msg]
  (when (> (count (keys msg)) 0)
    (cond
     (not (nil? (:track msg))) (render-track (:track msg))
     (not (nil? (:loves msg))) (add-love (:profile msg) (:loves msg))
     (not (nil? (:related msg))) (add-related (:profile msg) (:related msg))
     (not (nil? (:hookup msg))) (add-hookup (:profile msg) (:hookup msg))
     :else (println "unknown msg," msg))))
#_ (handle-message {:track {:uri "spotify:track:3tj5523lMxVh6TjcAb1tdS"
                            :name "track name"
                            :artist "artist name"}})
#_ (handle-message {:hookup {:uri "spotify:artist:1WvvwcQx0tj6NdDhZZ2zZz"
                             :name ""
                             :artist "future islands"}
                    :profile {:fbid "7812365"
                              :first-name "Cathleen"
                              :gender "female"}})

(defn set-fbid [{:keys [fbid]}]
  (def my-fbid fbid)
  (dommy/set-attr! (sel1 :#me) :src (str "http://graph.facebook.com/" fbid "/picture")))
(ajax/GET "/api/me"
             {:response-format :json
              :keywords? true
              :handler set-fbid
              :error-handler #(println "error!" %)})

(go-loop []
         (ajax/GET "/api/messages"
             {:response-format :json
              :keywords? true
              :handler handle-message
              :error-handler #(println "error!" %)})
         (<! (timeout 1000))
         (recur))
