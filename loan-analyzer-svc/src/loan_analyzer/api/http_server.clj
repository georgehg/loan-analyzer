(ns loan-analyzer.api.http-server
  {:clj-kondo/config '{:linters {:unresolved-symbol {:exclude [context ex]}}}}
  (:require [cheshire.core :as json]
            [io.pedestal.connector :as conn]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.cors :as cors]
            [io.pedestal.http.jetty :as jetty]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.error :as error-int]
            [io.pedestal.log :as logger]
            [io.pedestal.service.interceptors :as interceptors]
            [loan-analyzer.api.routes :as routes]))

(def ^:private server-error-dispatcher
  (error-int/error-dispatch [context ex]

                            [{:exception-type :invalid-request-error}]
                            (assoc context
                                   :response
                                   {:status 400
                                    :body  (:error-details (ex-data ex))})

                            :else
                            (assoc context
                                   :response
                                   {:status 500
                                    :body "Internal error. Please try again later."})))


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
   (body-params/body-params)
   json-output-encoder
   server-error-dispatcher])

(defn- create-http-server
  []
  (-> (conn/default-connector-map "localhost" 8090)
      (conn/with-interceptors service-interceptors)
      (conn/with-routes routes/routes)
      (jetty/create-connector {})))

(defonce *connector (atom nil))

(defn start-server
  [mode]
  (let [server (create-http-server)]
    (if (= :test mode)
      (reset! *connector server)
      (do (reset! *connector (conn/start! server))
          (logger/info :started :http-server :port 8090)))))

(defn stop-server
  []
  (when @*connector
    (conn/stop! @*connector)
    (reset! *connector nil)))
