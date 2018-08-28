(ns montoux.test.async-tests
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [<! >!]]
            [cljs.test :refer [async deftest is use-fixtures]]))

(use-fixtures :once {})

(deftest test-async
  (async done
    (let [events (async/chan)
          listen (go (loop [result []]
                       (if-let [r (<! events)]
                         (recur (conj result r))
                         result)))
          ]
      (go (>! events :start)
          (>! events :stop)
          (async/close! events)
          (is (= [:start :stop] (<! listen)))
          (done)))))
