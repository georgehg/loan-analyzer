(ns loan-analyzer.api.schemas
  (:require
   [io.pedestal.interceptor :as interceptor]
   [malli.core :as malli]
   [malli.error :as me]))

(def loan-application-sch
  (malli/schema
   [:map
    [:firstName :string]
    [:age [:int {:min 18 :max 120}]]
    [:income [:double {:min 0}]]
    [:creditScore [:int {:min 300 :max 850}]]
    [:requestedAmount [:double {:min 1000}]]
    [:employmentStatus[:enum "employed" "self-employed" "unemployed"]]
    [:employmentYears [:double {:min 0}]]]))

(defn validate-schema
  [schema data]
  (malli/validate schema data))

(defn human-explain
  [schema data]
  (-> (malli/explain schema data)
      (me/humanize)))

(defn- validate-input
  [schema params msg]
  (when-not (validate-schema schema params)
    (throw (ex-info msg
                    {:exception-type :invalid-request-error
                     :error-details {:message msg
                                     :error {:validation-errors (human-explain schema params)}}}))))

(defn input-validation-interceptor
  [schema]
  (interceptor/interceptor
   {:name :validate-request-body
    :enter (fn [context]
             (println "body: " (get-in context [:request :json-params]))
             (validate-input schema
                             (get-in context [:request :json-params])
                             "Invalid request input")
             context)}))
