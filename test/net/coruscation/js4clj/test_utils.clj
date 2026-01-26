(ns net.coruscation.js4clj.test-utils
  (:require
   [clojure.test :refer :all]
   [net.coruscation.js4clj.context :as context]))

(defn fresh-context [test]
  (context/reinitialize-context!)
  (test))
