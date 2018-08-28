(ns montoux.test.html
  (:require-macros [montoux.test.html :refer [intern]])
  (:require [cljs.test :as test]
            [cljsjs.jsdiff]
            [om.core :as om :include-macros true]
            [om.dom :as dom]))

(defonce ^:dynamic *state* (atom nil))
(defonce ^:dynamic *results* (atom nil))
(defonce ^:dynamic *data* (atom nil))

(defn update-data! []
  (let [data (reduce (fn [data [path value]]
                       (let [key  (last path)
                             path (butlast path)]
                         (if (get-in data path)
                           (update-in data path assoc key value)
                           data)))
                     @*results*
                     @*state*)]
    (reset! *data* data)))

(defonce
  initialised?
  (do (add-watch *state* :data (fn [& _] (update-data!)))
      (add-watch *results* :data (fn [& _] (update-data!)))
      true))

(defn- update-in-ns [m ns f & args]
  (apply update-in m [:namespaces ns] f args))

(defn- update-in-var [m var f & args]
  (let [{:keys [ns name]} (meta var)]
    (apply update-in m [:namespaces ns :vars name] f args)))

(deftype SummaryNode [data owner]
  om/IRender
  (render [this]
    (let [{:keys [pass fail error]} data
          total         (+ pass fail error)
          success-rate  (str (if (< 0 total)
                               (Math/round (* 100 (/ (float pass) total)))
                               100) "%")
          success-ratio (str pass "/" total)
          summary       (str "Pass: " pass ", Fail: " fail ", Error: " error)
          ]
      (dom/div #js {:className "summary"
                    :title     summary}
        (dom/div #js {:className "summary_bar"}
          (dom/div #js {:className "success"
                        :style     #js {:width success-rate}}))
        (dom/div #js {:className "summary_percent"}
          (dom/span nil success-rate))
        (dom/div #js {:className "summary_ratio"}
          (dom/span nil success-ratio))
        ))))

(deftype DiffToken [token owner]
  om/IRender
  (render [this]
    (let [{:keys [value removed? added?]} token
          class-name (cond removed?
                           "removed"
                           added?
                           "added"
                           :else
                           "unchanged")]
      (dom/span #js {:className class-name}
                (str value)))))

(deftype DiffNode [data owner]
  om/IRender
  (render [this]
    (if (and (list? data)
             (= 'not (first data))
             (list? (second data))
             (= 3 (count (second data)))
             (= '= (first (second data))))
      (let [[_ [_ a b]] data
            diff          (js/JsDiff.diffWords (pr-str a) (pr-str b))
            indexed-diffs (map-indexed (fn [index token]
                                         {:value    (aget token "value")
                                          :removed? (aget token "removed")
                                          :added?   (aget token "added")
                                          :index    index})
                                       diff)]
        (apply dom/pre #js {:className "diff"}
               (om/build-all ->DiffToken indexed-diffs {:key :index})))
      (dom/pre nil
        (pr-str data)))))

(deftype TestNode [data owner]
  om/IRender
  (render [this]
    (let [{:keys [type var line column expected actual context stack message]} data
          ;; CLJS currently does a very poor job of reporting assertion locations, see CLJS-1255
          {:keys [file line column] test-ns :ns test-name :name} (meta var)
          about (str (.toUpperCase (name type)) " in " stack)
          descr (str test-ns "/" test-name ":" line ":" column)
          url   (str "idea://open?file=" file "&line=" line)
          ok?   (= type :pass)]
      (dom/div #js {:className (str "test " (name type))}
        (dom/div #js {:className "test_about"}
          (dom/span nil about)
          (dom/a #js {:className "test_location" :href url} descr))
        (when (not ok?)
          (dom/div #js {:className "test_more"}
            (when context (dom/div nil (str context)))
            (when message (dom/div nil message))
            (dom/table #js {:className "test_body"}
              (dom/tbody nil
                (dom/tr nil
                  (dom/th nil "Expected: ")
                  (dom/td #js {:className "test_output"}
                    (dom/pre nil (pr-str expected))))
                (dom/tr nil
                  (dom/th nil "Actual: ")
                  (dom/td #js {:className "test_output"}
                    (om/build ->DiffNode actual)))))))
        ))))

(deftype VarNode [data owner]
  om/IRender
  (render [this]
    (let [{:keys [name var tests summary show?]} data
          ns    (-> var meta :ns)
          tests (filter (comp #{:fail :error} :type) tests)
          fail? (< 0 (+ (:fail summary) (:error summary)))
          pass? (and (< 0 (:pass summary)) (not fail?))
          ]
      (dom/div #js {:className (str "var"
                                    (when (not-empty tests) " has-children")
                                    (when pass? " pass")
                                    (when fail? " fail"))}
        (dom/div #js {:className "header"
                      :onClick   (fn [e] (swap! *state* update [:namespaces ns :vars name :show?] not))}

          (dom/div nil
            (dom/a #js {:href    (str "#" ns "/" name)
                        :onClick (fn [e] (.stopPropagation e))}
                   (str name)))
          (om/build ->SummaryNode summary))
        (when show?
          (apply dom/div #js {:className "var_tests"}
                 (om/build-all ->TestNode
                               (map-indexed #(assoc %2 :index %1) tests)
                               {:key :index})))
        ))))

(deftype NamespaceNode [data owner]
  om/IRender
  (render [this]
    (let [{:keys [ns summary vars show?]} data
          fail? (< 0 (+ (:fail summary) (:error summary)))
          pass? (and (< 0 (:pass summary)) (not fail?))
          ]
      (dom/div #js {:className (str "namespace"
                                    (when (not-empty vars) " has-children")
                                    (when pass? " pass")
                                    (when fail? " fail"))}
        (dom/div #js {:className "header"
                      :onClick   (fn [e] (swap! *state* update [:namespaces ns :show?] not))}
          (dom/div nil
            (dom/a #js {:href    (str "#" ns)
                        :onClick (fn [e] (.stopPropagation e))}
                   (str ns)))
          (om/build ->SummaryNode summary))
        (when show?
          (apply dom/div #js {:className "namespace_vars"}
                 (om/build-all ->VarNode
                               (for [[name data] (sort-by (comp :line meta :var second) vars)]
                                 (assoc data :name name))
                               {:key :name})
                 ))
        ))))

(deftype TestRoot [data owner]
  om/IRender
  (render [this]
    (let [{:keys [summary namespaces]} data
          {:keys [pass fail error test]} summary
          num-ns   (count namespaces)
          num-vars test
          num-test (+ pass fail error)
          descr    (str num-ns " namespaces, " num-vars " tests, " num-test " assertions")
          fail?    (< 0 (+ fail error))
          pass?    (and (< 0 pass) (not fail?))
          ]
      (dom/div #js {:className (str "root"
                                    (when pass? " pass")
                                    (when fail? " fail"))}
        (dom/div #js {:className "header"}
          (dom/div nil
            (dom/a #js {:href "#"} descr))
          (om/build ->SummaryNode summary)
          )
        (apply dom/div #js {:className "root_namespaces"}
               (om/build-all ->NamespaceNode
                             (for [[ns data] (sort-by first namespaces)]
                               (assoc data :ns ns))
                             {:key :ns}))))))


(defn- summarise-var [{:keys [tests] :as var}]
  (let [{:keys [pass fail error]} (group-by :type tests)]
    (assoc var :summary {:pass  (count pass)
                         :fail  (count fail)
                         :error (count error)
                         :tests 1})))

(defn- summarise-ns [{:keys [vars] :as ns}]
  (->> (vals vars)
       (mapcat :summary)
       (reduce (fn [m [k v]] (update m k + v)) {})
       (assoc ns :summary)))

(defn- with-context [m]
  (let [{:keys [testing-vars testing-contexts]} (test/get-current-env)]
    (assoc m :var (first testing-vars)
             :stack (test/testing-vars-str m)
             :context (test/testing-contexts-str))))

(defmethod test/report [::report :begin-test-ns] [{:keys [ns]}]
  (swap! *results* update-in-ns ns (constantly nil)))

(defmethod test/report [::report :begin-test-var] [{:keys [var]}]
  (swap! *results* update-in-var var assoc :var var :tests []))

(defmethod test/report [::report :pass] [m]
  (let [{:keys [var] :as m} (with-context m)]
    (swap! *results* update-in-var var update :tests conj m)))

(defmethod test/report [::report :fail] [m]
  (let [{:keys [var] :as m} (with-context m)]
    (swap! *results* update-in-var var update :tests conj m)))

(defmethod test/report [::report :error] [m]
  (let [{:keys [var] :as m} (with-context m)]
    (swap! *results* update-in-var var update :tests conj m)))

(defmethod test/report [::report :end-test-var] [{:keys [var]}]
  (swap! *results* update-in-var var summarise-var))

(defmethod test/report [::report :end-test-ns] [{:keys [ns]}]
  (swap! *results* update-in-ns ns summarise-ns))

(defmethod test/report [::report :summary] [m]
  (swap! *results* assoc :summary (select-keys m [:fail :error :pass :test])))

(def styles (intern "montoux/test/report.css"))

(defn wrapper [id]
  (or (.getElementById js/document id)
      (let [$el (.createElement js/document "DIV")]
        (set! (.-id $el) id)
        (doto (.-body js/document)
          (.appendChild $el))
        $el)))

(deftype Style [data parent]
  om/IRender
  (render [this]
    (dom/style #js {:scoped                  nil
                    :dangerouslySetInnerHTML #js {:__html (str data)}})))

(defn url-filter []
  (let [hash (or (.substr (.-hash js/location) 1) "")
        [ns-name var-name] (.split hash "/")]
    (fn [nsp]
      (or (empty? ns-name)
          (= ns-name (name nsp))))))

(defn initialise
  "Modify the given test environment to use the url-filter and capture reports so they can be rendered."
  [env]
  (reset! *results* nil)
  (let [$style (wrapper "test-styles")]
    (when (= "DIV" (.-nodeName $style))
      ;; only inject styles when document doesn't define a style node already
      (om/root ->Style styles {:target $style})))
  (om/root ->TestRoot *data* {:target (wrapper "test-root")})
  (let [env (update env :reporters conj ::report)]
    (if (contains? env :filter)
      (update env :filter every-pred (url-filter))
      (assoc env :filter (url-filter))
      )))

(defn on-url-change
  "Run the given function every time the URL changes."
  [run-tests]
  (set! (.-onhashchange js/window) (fn [] (run-tests))))
