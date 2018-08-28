(ns test.html
  (:require [montoux.test.async-tests]
            [montoux.test.core :as core :include-macros true]
            [montoux.test.mock-tests]
            [montoux.test.html :as html]
            [montoux.test.test-tests]
            ))

(enable-console-print!)

(core/deftests tests)

(defn run-tests []
  (core/run-tests tests (html/initialise (core/default-env))))

(html/on-url-change #'run-tests)

(run-tests)
