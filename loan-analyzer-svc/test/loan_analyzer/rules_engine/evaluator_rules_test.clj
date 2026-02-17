(ns loan-analyzer.rules-engine.evaluator-rules-test 
  (:require
   [clojure.test :refer [deftest is testing]]
   [loan-analyzer.rules-engine.engine :as sut]))

(deftest tests-loan-application-denied-decision
  (testing "Should return only denied decision for loan application"
    (is (= {:decision "denied"
            :reasons [{:code "VERY_LOW_CREDIT_SCORE"
                       :message "Credit score < 600"}]}
           (sut/evaluate-loan-application {:creditScore 500
                                           :income 20000})))))

(deftest tests-loan-application-review-decisions
  (testing "Should return only review decision for loan application"
    (is (= {:decision "review"
            :reasons [{:code "LOW_CREDIT_SCORE"
                       :message "Credit score < 650"}
                      {:code "LOW_INCOME"
                       :message "Income < 30000"}
                      {:code "INSUFFICIENT_EMPLOYMENT_HISTORY"
                       :message "Employment years < 1"}]}
           (sut/evaluate-loan-application {:creditScore 620
                                           :income 2000
                                           :employmentYears 0.5})))))

(deftest tests-loan-application-approved-decision
  (testing "Should return only approved decision for loan application"
    (is (= {:decision "approved"
            :reasons []}
           (sut/evaluate-loan-application {:creditScore 1000
                                           :income 50000
                                           :employmentYears 2})))))
