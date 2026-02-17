(ns loan-analyzer.api.http-server
  (:require [cheshire.core :as json]
            [io.pedestal.connector :as conn]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.cors :as cors]
            [io.pedestal.http.jetty :as jetty]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.log :as logger]
            [io.pedestal.service.interceptors :as interceptors]
            [loan-analyzer.api.routes :as routes]))

(def ^:private json-output-encoder
  (interceptor/interceptor
   {:name  :json-output-encoder
    :leave (fn [context]
             (-> context
                 (update-in [:response :body] json/encode)
                 (update-in [:response :headers] merge {"Content-Type" "application/json"})))}))

(def ^:private service-interceptors
  [interceptors/log-request
   (cors/allow-origin (constantly true))
   interceptors/not-found
   route/query-params
   (body-params/body-params)
   json-output-encoder])

(defn- create-http-server
  []
  (-> (conn/default-connector-map "localhost" 8090)
      (conn/with-interceptors service-interceptors)
      (conn/with-routes routes/routes)
      (jetty/create-connector {})))

(defonce *connector (atom nil))

(defn start-server
  []
  (reset! *connector
          (conn/start! (create-http-server)))
  (logger/info :started :http-server :port 8090))

(defn stop-server
  []
  (when @*connector
    (conn/stop! @*connector)
    (reset! *connector nil)))
