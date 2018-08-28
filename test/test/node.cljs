(ns test.node
  (:require [montoux.test.core :as core :include-macros true]
            [montoux.test.node :as node]
            [montoux.test.async-tests]
            [montoux.test.mock-tests]
            [montoux.test.test-tests]))

(core/deftests tests)

(defn -main [& args]
  (enable-console-print!)
  (core/run-tests tests (core/default-env node/report)))

(set! *main-cli-fn* -main)
