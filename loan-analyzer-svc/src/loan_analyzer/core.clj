(ns loan-analyzer.core
  (:require [loan-analyzer.api.http-server :as server]))

(defn start-service
  []
  (server/start-server))

(defn stop-service
  []
  (server/stop-server))

(defn -main [& _args]
  (java.util.TimeZone/setDefault (java.util.TimeZone/getTimeZone "UTC"))
  (start-service)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(stop-service))))

(comment
  (do   (java.util.TimeZone/setDefault (java.util.TimeZone/getTimeZone "UTC"))
        (load-file "src/loan_analyzer/core.clj")
        (stop-service)
        (start-service)
        nil))
