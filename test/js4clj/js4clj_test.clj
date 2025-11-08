(ns js4clj.js4clj-test
  (:require [clojure.test :refer :all]
            [js4clj.require :as require]
            [js4clj.js :as js]
            [js4clj.utils :refer [js. js.. js.- clj->js js->clj]]))

(deftest require-test
  (testing ""
    (require/require-js '["luxon" :as lux]
                        '["luxon" :as lux2])
    (is (resolve 'lux/DateTime))
    (is (resolve 'lux2/DateTime))))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= (:as (#'require/parse-flags '[:as name]))
           'name))
    (is (:load-ns (#'require/parse-flags '[:load-ns :as name])))
    (is (= (:as (#'require/parse-flags '[:load-ns :as name]))
           'name))
    (is (let [{:keys [f1 f2 f3 f4 load-ns as]} (#'require/parse-flags '[:f1 :f2 :load-ns :as name :f3 :f4])]
          (and f1 f2 f3 f4 load-ns (= as 'name))))))

(require/require-js '["luxon" :as lux])

(deftest js-test
  (testing ""
    (let [test-dt (js. lux/DateTime fromISO "2017-05-15T08:30:00Z")]
      (is (= (js.- test-dt year)
             2017)))))

(deftest clj->js-test
  (testing ""
    (let [dt (js. lux/DateTime fromObject
               (clj->js {:year 2012 :day 22 :hour 12})
               (clj->js {:zone "America/Los_Angeles"
                         :numberingSystem "beng"}))]
      (is (= (:year (:c (js->clj dt :keywordize-keys true)))
             2012))
      (is (= (:year (:c (js->clj dt :keywordize-keys true)))
             2012)))))

(deftest test-pass-clojure-fn
  (testing ""
    (let [testing-vec [1 2 3 4 5 6]
          double (fn [x & _] (* x 2))
          sum-of-two (fn [a b & _] (+ a b))]
      (is (= (js->clj
              (js. (clj->js testing-vec) map
                double))
             (map double testing-vec)))

      (is (= (js. (clj->js testing-vec) reduce sum-of-two 0)
             (reduce sum-of-two 0 testing-vec))))))
