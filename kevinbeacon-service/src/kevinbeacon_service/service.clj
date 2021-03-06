(ns kevinbeacon-service.service
  (:require [io.pedestal.service.http :as bootstrap]
            [io.pedestal.service.http.route :as route]
            [io.pedestal.service.http.body-params :as body-params]
            [io.pedestal.service.http.route.definition :refer [defroutes]]
            [io.pedestal.service.http.ring-middlewares :as middlewares]
            [io.pedestal.service.interceptor :refer [defhandler definterceptor]]
            [ring.util.response :as ring-resp]
            [ring.middleware.session.cookie :as cookie]
            [kevinbeacon-service.handlers :as handlers]))

(definterceptor session-interceptor
  (middlewares/session {:store (cookie/cookie-store)}))

(defroutes routes
  [[["/" ^:interceptors [session-interceptor]
     ["/api" ^:interceptors [bootstrap/json-body
                                       middlewares/keyword-params
                                       (body-params/body-params)]
      ["/register" {:post handlers/handle-register}]
      ["/next-track" {:get handlers/handle-next-track}]
      ["/messages" {:get handlers/handle-get-message}]
      ["/me" {:get handlers/handle-me}]]]]])

(def service {:env :prod
              ::bootstrap/routes routes
              ::bootstrap/resource-path "/public"
              ::bootstrap/type :jetty
              ::bootstrap/port 8080})
