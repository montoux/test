(ns montoux.test.core
  (:require [cljs.analyzer.api :as ana-api]
            [cljs.test :as test]))

(defn- all-test-namespaces
  "Generates a list of quoted symbols that refer to namespaces containing test vars. Namespaces are sorted."
  []
  (let [ns-syms (sort (ana-api/all-ns))]
    (eduction
      (filter #(boolean (some (comp :test val) (ana-api/ns-publics %))))
      (map (partial list 'quote))
      ns-syms)))

(defn- update-summary
  "Generates a test block that accumulates results from the previous block into summary, then clears counters.
  Based on `cljs.test/run-tests-block`."
  [summary]
  `(fn []
     (vswap!
       ~summary
       (partial merge-with +)
       (:report-counters (test/get-and-clear-env!)))))

(defn- end-run
  "Generates a summary block, for use in `run-tests-block. Based on `cljs.test/run-tests-block."
  [env summary]
  `(fn []
     (test/set-env! ~env)
     (test/do-report (deref ~summary))
     (test/report (assoc (deref ~summary) :type :end-run-tests))
     (test/clear-env!)))

(defmacro deftests
  "Returns a function that consumes a test env and returns a list of fns that can be passed to `cljs.test/run-block`.
  Unlike `cljs.test/run-tests-block`, this runner filters test namespaces at runtime using a function instead of relying
  on compile-time filtering based on a regex. Provide a namespace symbol filter predicate by adding `:filter` to env."
  [sym]
  (let [env     (gensym "env")
        filter  (gensym "filter")
        reset   (gensym "reset")
        summary (gensym "summary")
        result  (gensym "result")
        initial [summary `(volatile! {:type :summary :test 0 :pass 0 :fail 0 :error 0})
                 reset (update-summary summary)
                 filter `(get ~env :filter (constantly true))
                 result []]
        binding (transduce
                  (map (fn [ns]
                         `(if (~filter ~ns)
                            (conj (into ~result (test/test-ns-block ~env ~ns)) ~reset)
                            ~result)))
                  (completing #(conj %1 result %2))
                  initial
                  (all-test-namespaces))
        binding (conj binding result `(conj ~result ~(end-run env summary)))]
    `(defn ~sym [~env]
       (let ~binding
         ~result))))
