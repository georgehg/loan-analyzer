(ns loan-analyzer.api.routes
  (:require
   [loan-analyzer.api.schemas :refer [input-validation-interceptor
                                      loan-application-sch]]
   [loan-analyzer.rules-engine.engine :as evaluator]))

(defn- handle-loan-evaluation-fn
  [context]
  (let [loan-data (get-in context [:request :json-params])]
  (assoc context
         :response
         {:status 200
          :body  (evaluator/evaluate-loan-application loan-data)})))

(def handle-loan-evaluation
  {:name :handle-loan-evaluation
   :enter handle-loan-evaluation-fn})

(def loan-evaluation-service
  [(input-validation-interceptor loan-application-sch)
   handle-loan-evaluation])

(def routes #{["/api/v1/evaluate" :post loan-evaluation-service  :route-name :post-loan-evaluation]})
