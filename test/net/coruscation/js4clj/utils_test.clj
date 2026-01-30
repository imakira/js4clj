(ns net.coruscation.js4clj.utils-test
  (:require
   [clojure.test :refer [deftest is]]
   [net.coruscation.js4clj.utils :as subject :refer [clj->js]]))

(deftest js-select-keys-test
  ;; use string as key, return string as key
  (is (= {"a" 1}
         (subject/js-select-keys (clj->js {:a 1})
                                 ["a"])))
  ;; you can also use keyword as key, return keyword as key
  (is (= {:a 1}
         (subject/js-select-keys (clj->js {:a 1})
                                 [:a])))

  (is (= {:a 1}
         (subject/js-select-keys (clj->js {:a 1})
                                 [:a :b :c])))

  (is (= {:a 1 :b 2}
         (subject/js-select-keys (clj->js {:a 1 :b 2})
                                 [:a :b :c])))
  ;; key can be determined at run time
  (let [key :a]
    (= {:a 1} (subject/js-select-keys (clj->js {:a 1})
                                      [key]))))

(deftest select-keys-bind-test
  (subject/select-keys-bind
      [a b c]
      (clj->js {:a 1 :b 2 :c 3})
    (is (= [1 2 3]
           [a b c]))))
