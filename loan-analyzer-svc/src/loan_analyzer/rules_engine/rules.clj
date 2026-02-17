(ns loan-analyzer.rules-engine.rules
  (:require
   [clara.rules :refer [defquery defrule insert!]]
   [clara.rules.accumulators :as acc]))

(defrecord LoanApplication [credit-score income employment-years])

(defrecord Decision [decision reason weight])

(def decisions
  {:decision/approved {:decision "approved" :weight 1}
   :decision/review   {:decision "review"   :weight 2}
   :decision/denied   {:decision "denied"   :weight 3}})

(defn- ->decision
  ([decision]
   (->decision decision nil))
  ([decision reason]
   (map->Decision (assoc (decision decisions)
                         :reason reason))))


(defrule approved-loan
  =>
  (insert! (->decision :decision/approved)))

(defrule low-credit-score
  [LoanApplication (= ?credit-score credit-score)]
  [:test (< ?credit-score 650)]
  =>
  (insert! (->decision :decision/review
                       {:code "LOW_CREDIT_SCORE"
                        :message "Credit score < 650"})))

(defrule low-income
  [LoanApplication (= ?income income)]
  [:test (< ?income 30000)]
  =>
  (insert! (->decision :decision/review
                       {:code "LOW_INCOME"
                        :message "Income < 30000"})))


;; (defrule employment-hsitory
;;   [LoanApplication (= ?employment-years employment-years)]
;;   [:test (< ?employment-years 1)]
;;   =>
;;   (insert! (->Decision :review {:code "INSUFFICIENT_EMPLOYMENT_HISTORY"
;;                                  :message  "Employment years < 1"})))

(defrule very-low-credit-score
  [LoanApplication (= ?credit-score credit-score)]
  [:test (< ?credit-score 600)]
  =>
  (insert! (->decision :decision/denied
                       {:code "VERY_LOW_CREDIT_SCORE"
                        :message "Credit score < 600"})))


(def reasons-by-decisions
  "Aggregates reasons by decisions accumulating reasons"
  (acc/grouping-by :decision
                   (partial
                    reduce-kv
                    (fn [result decision reasons]
                      (conj result
                            {:decision decision
                             :reasons (reduce (fn [rs reason]
                                                (conj rs (:reason reason)))
                                              []
                                              reasons)}))
                    [])))

(defquery get-decisions
  []
  [?max-weight <- (acc/max :weight) :from [Decision]]
  [?decisions <- reasons-by-decisions :from [Decision (= weight ?max-weight)]])
