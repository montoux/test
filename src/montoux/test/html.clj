(ns montoux.test.html
  (:refer-clojure :exclude [intern])
  (:require [clojure.java.io :as io]))

(defmacro intern [path]
  (slurp (io/resource path)))
