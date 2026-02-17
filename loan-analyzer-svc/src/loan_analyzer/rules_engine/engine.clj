(ns loan-analyzer.rules-engine.engine
  (:require [clara.rules :refer [fire-rules insert mk-session query]]
            [loan-analyzer.rules-engine.rules :as rules]))

(defn- create-session
  []
  (mk-session 'loan-analyzer.rules-engine.rules))

(defn- insert-loan-fact
  [session {:keys [credit_score income employment_years]}]
  (insert session
          (rules/->LoanApplication credit_score income employment_years)))

(defn- query-decisions
  [session]
  (query session rules/get-decisions))

(defn evaluate-loan-application
  [application]
  (-> (create-session)
      (insert-loan-fact application)
      fire-rules
      query-decisions
      first
      :?decisions))
