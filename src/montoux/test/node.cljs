(ns montoux.test.node
  "Test reporter for node.js that exits the VM on test run completion."
  (:require [cljs.test :as test]))

(defn exit-code
  "Returns 0 on success and 1 on failure."
  [m]
  (if (and (zero? (:fail m))
           (zero? (:error m)))
    0
    1))

(defmethod test/report [::report :end-run-tests] [m]
  (.exit js/process (exit-code m)))

(def report ::report)
