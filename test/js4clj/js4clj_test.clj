(ns js4clj.js4clj-test
  #_{:clj-kondo/ignore [:refer-all]}
  (:require [clojure.test :refer :all]
            [js4clj.require :as require]
            [js4clj.core :as core]
            [js4clj.js :as js]
            [js4clj.utils :refer [js. js.. js.- js-set! clj->js js->clj js-new]]))

(deftest require-test
  (testing "Requiring commonjs modules test"
    (require/require-js '["luxon" :as lux]
                        '["luxon" :as lux2])
    (is (resolve 'lux/DateTime))
    (is (resolve 'lux2/DateTime)))

  (testing "Requiring esm modules test"
    (require/require-esm '["luxon/build/es6/luxon.mjs" :as elux]
                         '["luxon/build/es6/luxon.mjs" :as elux2])
    (is (resolve 'elux/DateTime))
    (is (resolve 'elux2/DateTime))
    (is (string? (js.. (var-get (resolve 'elux/DateTime)) now toString)))))

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

(deftest wrapper-method-test
  (testing ""
    (is (function? js/Array))
    (is (= (core/get-meta-qualified-name (core/get-meta-object js/Array))
           "Function"))
    (is (= (core/get-meta-qualified-name (core/get-meta-object (core/get-member js/Array "from")))
           "Function"))))

(deftest null-undefined-test
  (testing ""
    (is (core/js-undefined? js/undefined))
    (is (false? (core/js-undefined? nil)))
    (is (false? (core/js-undefined? (clj->js nil))))))

(deftest polyglot-value?-test
  (testing ""
    (is (true? (fn? js/Array)) "js/Array is a clojure fn")
    (is (boolean (core/polyglot-value js/Array)) "js/Array is also a polyglot value")
    (is (isa? (class (core/polyglot-value js/Array))
              org.graalvm.polyglot.Value) "It returns the wrapped Value")))

(deftest js-array?-test
  (testing ""
    (is (true? (core/js-array? (clj->js [1 2 3]))) "it is a js array")
    (is (false? (core/js-array? nil)) "returns false, no error threw")
    (is (false? (core/js-array? js/undefined)) "returns false, no error threw")))

(deftest js->clj-test
  (testing "Default testing"
    (is (nil? (js->clj js/undefined)))
    (is (fn? (js->clj js/Array))))

  (testing "Testing builitn types, they should be returned as is"
    (is (nil? (js->clj nil)))
    (is (= (js->clj 0) 0))
    (is (= (js->clj {:a 1})
           {:a 1}))
    (is (= (js->clj [1 2 3])
           [1 2 3]))))

(deftest js-set!-test
  (testing ""
    (let [obj (clj->js {:level1 {:level2 {:key nil}}})]
      (js-set! (js.. obj -level1 -level2 -key) 1)
      (is (= 1 (js.. obj -level1 -level2 -key)))
      (js-set! (js.- (js.. obj -level1 -level2)
                     key)
               2)
      (is (= 2 (js.. obj -level1 -level2 -key))))))

(deftest js-new-test
  (testing ""
    (is (= (js->clj (js-new js/Array 1 2 3 4))
           [1 2 3 4]))))

(deftest clj->js-test
  (testing ""
    (let [dt (js. lux/DateTime fromObject
               (clj->js {:year 2012 :day 22 :hour 12})
               (clj->js {:zone "America/Los_Angeles"
                         :numberingSystem "beng"}))]
      (is (= (:year (:c (js->clj dt :keywordize-keys true)))
             2012))
      (is (= (:year (:c (js->clj dt :keywordize-keys true)))
             2012))))

  (testing ""
    (is (isa? (class (clj->js nil))
              org.graalvm.polyglot.Value))
    (is (isa? (class (clj->js 0))
              org.graalvm.polyglot.Value))
    (is (isa? (class (clj->js (fn [])))
              org.graalvm.polyglot.Value)))

  (testing "Testing wrapped clojure fn"
    (is (isa? (class (clj->js (fn [])))
              org.graalvm.polyglot.Value))
    (is (fn? (js->clj (clj->js (fn [])))))

    (is (= ((js->clj (clj->js +)) 1 2 3)
           6)))

  (testing "Testing unwrapped host java obj"
    (is (isa? (class (clj->js {}))
              org.graalvm.polyglot.Value))
    (is (map? (js->clj (clj->js {}))))))

(deftest pass-clojure-fn-test
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

(deftest executable-javascript-object-test
  (testing ""
    (is (function? js/Array) "It is a clojure function")
    (is (core/js-fn? js/Array) "It is also a javascript function")
    (is (core/can-instantiate js/Array)  "We can also instantiate it")
    (is (core/get-member js/Array "from") "We can get its static method")
    (is (= (js->clj (js. js/Array from (clj->js [1 2 3])))
           [1 2 3]) "We can also call its static method")))

