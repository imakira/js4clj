(ns net.coruscation.js4clj.test-embedding
  (:gen-class)
  (:require
   [net.coruscation.js4clj.require :refer [require-cjs]]
   [net.coruscation.js4clj.utils :refer [js..]]))

(require-cjs '["luxon" :as luxon])

(defn -main [& _]
  (println (js.. luxon/DateTime now toString)))
