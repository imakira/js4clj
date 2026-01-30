(ns net.coruscation.js4clj.core-test
  (:require
   [clojure.test :refer [deftest is]]
   [net.coruscation.js4clj.core :refer [js-aget js-aset]]
   [net.coruscation.js4clj.utils :refer [clj->js]]))

(deftest js-aget-test
  (let [obj-js (clj->js {:a 1})]
    (is (= 1 (js-aget obj-js "a")))
    (js-aset obj-js "a"
             2)
    (is (js-aget obj-js "a")
        2)))
