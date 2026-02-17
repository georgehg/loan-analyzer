(ns loan-analyzer.api.routes-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [io.pedestal.connector.test :refer [response-for]]
   [loan-analyzer.api.http-server :as server]
   [loan-analyzer.core :refer [start-service stop-service]]))

(defn server-fixture
  [f]
  (start-service :test)
  (try
    (f)
    (finally
      (stop-service))))

(use-fixtures :once server-fixture)

(defn- response-for-service
  [verb endpoint body]
  (response-for @server/*connector
                verb
                endpoint
                :headers {:content-type "application/json"}
                :body body))

(deftest tests-post-loan-application-evaluation-success
  (testing "Should return approved loan application"
    (let [{:keys [status body]}
          (response-for-service :post
                                "/api/v1/evaluate"
                                (json/encode {:firstName "John Smith"
                                              :age 45
                                              :income 30000.90
                                              :creditScore 770
                                              :requestedAmount 10000.00
                                              :employmentStatus "self-employed"
                                              :employmentYears 10.0}))]

      (is (= 200 status))
      (is (= {:decision "approved"
              :reasons []}
             (json/decode body keyword)))))

  (testing "Should return review loan application"
    (let [{:keys [status body]}
          (response-for-service :post
                                "/api/v1/evaluate"
                                (json/encode {:firstName "John Smith"
                                              :age 45
                                              :income 30000.90
                                              :creditScore 770
                                              :requestedAmount 10000.00
                                              :employmentStatus "self-employed"
                                              :employmentYears 0.5}))]

      (is (= 200 status))
      (is (= {:decision "review"
              :reasons [{:code "INSUFFICIENT_EMPLOYMENT_HISTORY", :message "Employment years < 1"}]}
             (json/decode body keyword))))))

(deftest tests-post-loan-application-evaluation-failed
  (testing "Should return Bad Request error for invalid loan application"
    (let [{:keys [status body]}
          (response-for-service :post
                                "/api/v1/evaluate"
                                (json/encode {:firstName "John Smith"
                                              :age 45
                                              :income 30000.90
                                              :creditScore 770
                                              :requestedAmount 10000.00
                                              :employmentStatus "student"}))]

      (is (= 400 status))
      (is (= {:message "Invalid request input"
              :error {:validation-errors {:employmentStatus ["should be either \"employed\", \"self-employed\" or \"unemployed\""],
                                          :employmentYears ["missing required key"]}}}
             (json/decode body keyword))))))
