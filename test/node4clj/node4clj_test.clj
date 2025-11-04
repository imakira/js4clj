(ns javascript-clj.javascript-clj-test
  (:require [clojure.test :refer :all]
            [javascript-clj.javascript-clj :as n4clj]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= (:as (#'n4clj/parse-flags '[:as name]))
           'name))
    (is (:load-ns (#'n4clj/parse-flags '[:load-ns :as name])))
    (is (= (:as (#'n4clj/parse-flags '[:load-ns :as name]))
           'name))
    (is (let [{:keys [f1 f2 f3 f4 load-ns as]} (#'n4clj/parse-flags '[:f1 :f2 :load-ns :as name :f3 :f4])]
          (and f1 f2 f3 f4 load-ns (= as 'name))))))
