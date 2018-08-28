(ns montoux.test.test-tests
  (:require [cljs.test :as test :refer-macros [deftest is test-ns testing]]
            [montoux.test.mock :as mock :refer [mock stub]]))

(defn throw! [throw?]
  (when throw?
    (throw (js/Error. "catch me!"))))

(deftest test-true
  (is true))

(deftest test-function
  (is (every? even? [(+ 1 3) (+ 5 7) (+ 9 11)])))

(deftest test-throw
  (is (thrown? js/Error (throw! true))))

(deftest test-every
  (is (every? even? [2 4])))

(deftest test-eq-fails
  (let [calls (binding [test/report (stub nil)]
                (is (= "abc" (apply str ["a" "b"])))
                (mock/calls test/report))
        call  (ffirst calls)]
    (is (= '(not (= "abc" "ab"))
           (:actual call)))
    (is (= :fail (:type call)))
    ))

#_(deftest ^:fails test-failing-test
    (testing "basic failing test:"
      (is (= 1 3))
      (is (= 1 2))
      (is (= "all good"
             "it's really not good")
          "compare diff: good is common, but all should not be found in 'really'")
      (is (= "not ill"
             nil)
          "compare diff: nil should not be found in 'not ill'")
      ))

#_(deftest ^:fails test-throwing-is
    (testing "throwing an exception from a 'is'"
      (is (= 1 (throw (js/Error. "This is an error"))))))

#_(deftest ^:fails test-throwing-var
    (testing "throwing an exception from a test var"
      (throw (js/Error. "This is an error"))))
