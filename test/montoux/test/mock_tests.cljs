(ns montoux.test.mock-tests
  (:require [cljs.test :refer-macros [deftest is]]
            [montoux.test.mock :as mock :refer [mock stub]]))

(defn ^:dynamic calc-x [x1 x2]
  (* x1 x2))

(defn ^:dynamic calc-y [y1 y2]
  (/ y1 y2))

(defn some-client []
  (+ (calc-x 2 3) (calc-y 6 3)))

(defn ^:dynamic log [& args]
  (is nil "this function should be mocked"))

(defn logging-fn []
  (log "fail")
  (log "fail again"))

(deftest no-stubbing
  (is (= 8 (some-client))))

(deftest stubbing
  (binding [calc-x (stub 1)
            calc-y (stub 2)]
    (is (= 3 (some-client)))
    (is (= [[2 3]]
           (mock/calls calc-x)))
    (is (= [[6 3]]
           (mock/calls calc-y)))
    (is (= [2 3]
           (mock/last-call calc-x)))
    (is (= [2 3]
           (mock/only-call calc-x)))
    ))

(deftest mocking
  (binding [calc-x (mock +)
            calc-y (mock -)]
    (is (= 8 (some-client))))

  (binding [log (mock (constantly nil))]
    (is (mock/not-called log))
    (logging-fn)
    (is (= [["fail"] ["fail again"]]
           (mock/calls log)))
    (is (= ["fail again"]
           (mock/last-call log)))
    (is (not (mock/only-call log)))
    (is (not (mock/not-called log)))

    (binding [log (mock (constantly nil))]
      (is (mock/not-called log)
          "new mock has been installed")
      (logging-fn)
      (log "ok")
      (is (= [["fail"] ["fail again"] ["ok"]]
             (mock/calls log))
          "inner mock captures calls"))

    (is (= [["fail"] ["fail again"]]
           (mock/calls log))
        "original mock is unaffected")
    ))
