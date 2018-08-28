(ns montoux.test.core
  (:require [cljs.test :as test]))

;; handle all test reporting will a single defmethod
(derive :pass ::message)
(derive :fail ::message)
(derive :error ::message)
(derive :summary ::message)
(derive :begin-test-ns ::message)
(derive :end-test-ns ::message)
(derive :begin-test-var ::message)
(derive :end-test-var ::message)
(derive :end-run-tests ::message)

(defmethod test/report [::dispatch ::message] [m]
  (let [{:keys [reporter reporters] :as env} (test/get-current-env)]
    (doseq [delegate reporters]
      (try
        (test/update-current-env! [:reporter] (constantly delegate))
        (test/report m)
        (finally (test/update-current-env! [:reporter] (constantly reporter)))
        ))))

(defn default-env
  "Creates a default test environment that supports dispatching to multiple reporters."
  [& reporters]
  (assoc (test/empty-env ::dispatch)
    :reporter ::dispatch
    :reporters (into [::test/default] reporters)))

(defn run-tests
  "Similar to cljs.test/run-all-tests, but with support for:
    * runtime filtering via `:filter`
    * multiple reporters via `:reporters`."
  ([tests]
   (run-tests tests (default-env)))
  ([tests env]
   (test/run-block (tests env))))
