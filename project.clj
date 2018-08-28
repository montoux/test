(defproject montoux/test "1.0.0-SNAPSHOT"
  :description "Testing utilities for Clojurescript, including interactive HTML test output."
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/montoux/test"
  :min-lein-version "2.8.0"

  :source-paths ["src"]
  :resource-paths ["resources"]
  :test-paths ["test"]
  :compile-path "target/classes"
  :target-path "target/"

  :dependencies [
                 ;; core
                 [org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]

                 [org.omcljs/om "1.0.0-alpha48" :scope "provided"]
                 [cljsjs/jsdiff "3.4.0-0" :scope "provided"]
                 ]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.16"]]

  :cljsbuild {:builds {}}

  :hooks [leiningen.cljsbuild]

  :profiles {:dev      {:cljsbuild
                        {:test-commands
                         {"node" ["node" "target/test/js/test.js"]}
                         :builds
                         {:test {:source-paths ["src" "test"]
                                 :compiler     {:output-to     "target/test/js/test.js"
                                                :output-dir    "target/test/js"
                                                :optimizations :none
                                                :main          "test.node"
                                                :target        :nodejs}}}}}
             :test     {:resource-paths ["test-resources"]
                        :resources      [[org.clojure/core.async "0.4.474"]]}
             :node     {:cljsbuild
                        {:builds
                         {:test {:notify-command ["node" "target/test/js/test.js"]}}}}
             :html     {:cljsbuild
                        {:builds
                         {:test {:compiler {:main   "test.html"
                                            :target :none}}}}}
             :figwheel {:resource-paths
                        ["target"]
                        :figwheel
                        {:http-server-root "test"
                         :css-dirs         ["src/montoux/test/report"]}
                        :cljsbuild
                        {:builds
                         {:test {:compiler ^:replace
                                           {:output-to            "target/test/js/test.js"
                                            :output-dir           "target/test/js"
                                            :optimizations        :none
                                            :main                 "test.figwheel"
                                            :asset-path           "js"
                                            :recompile-dependents true
                                            }}}}}
             }

  :aliases {"node" [["with-profile" "+test,+node"] ["cljsbuild" "auto"]]
            "html" [["with-profile" "+test,+html"] ["cljsbuild" "auto"]]
            "fig"  [["with-profile" "+test,+figwheel"] ["figwheel"]]}
  )
