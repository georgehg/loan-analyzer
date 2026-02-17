(ns loan-analyzer.api.routes
  (:require [loan-analyzer.rules-engine.engine :as evaluator]))

(defn- handle-loan-evaluation-fn
  [context]
  (let [loan-data (-> context :request :json-params)]
  (assoc context :response
         {:status 200
          :body  (evaluator/evaluate-loan-application loan-data)})))

(def handle-loan-evaluation
  {:name :handle-loan-evaluation
   :enter handle-loan-evaluation-fn})

(def routes #{["/api/v1//evaluate" :post handle-loan-evaluation  :route-name :post-loan-evaluation]})
