(ns montoux.test.mock
  "Clojurescript functions for mocking, based on examples from 'Clojure in action'.
  Functions to be mocked must be declared dynamic."
  {:author "Montoux Limited <info@montoux.com>"})

(defn- capture [f]
  (let [mock-calls (atom [])]
    (-> (fn [& args]
          (swap! mock-calls conj args)
          (apply f args))
        (vary-meta merge (meta f))
        (vary-meta assoc :mock-calls mock-calls))))

(defn mock
  "Constuct a mock that will capture arguments and return the result of applying `f` to the arguments."
  [f]
  (capture f))

(defn stub
  "Construct a mock that will capture arguments while always returning `v`."
  [v]
  (capture (constantly v)))

(defn fail
  ([] (fail nil))
  ([message]
   (fn fail* [& args]
     (cond
       (instance? js/Error message)
       (throw message)
       (string? message)
       (throw (js/Error. message))
       :else
       (throw (js/Error. "fail"))))))

(defn calls [f]
  (-> f meta :mock-calls deref))

(defn only-call
  "Return the arguments to the only call to `f`, or nil if there was not exactly one call to `f`. `f` must be a mock."
  [f]
  (let [call-log (calls f)]
    (when (= 1 (count call-log))
      (first call-log))))

(defn last-call
  "Return the arguments to the most recent call to `f`, or nil if there was no call. `f` must be a mock."
  [f]
  (last (calls f)))

(defn not-called
  "Return true iff `f` was not called. `f` must be a mock."
  [f]
  (empty? (calls f)))
