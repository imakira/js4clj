(ns js4clj.components
  #?(:cljs (:require-macros [js4clj.react-wrapper :refer [defcomp]]))
  (:require [js4clj.react-wrapper :refer [r use-state defcomp]]))

(defcomp counter [{:keys [count]}]
  (r "span" {} (str "count: " count)))

(defcomp ops [{:keys [incr]}]
  (r "button" {:onClick incr}
     "incr"))

(defcomp home []
  (let [[count set-count!] (use-state 1)]
    (r "div" {}
       (r "h1" {:className "class"}
          "Bonjour")
       (r ops {:incr (fn [_]
                       (set-count! (+ count 1)))})
       (r counter {:count count}))))
