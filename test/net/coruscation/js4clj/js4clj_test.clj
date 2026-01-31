(ns net.coruscation.js4clj.js4clj-test
  #_{:clj-kondo/ignore [:refer-all]}
  (:require [clojure.test :refer :all]
            [net.coruscation.js4clj.api.converting :as converting]
            [net.coruscation.js4clj.api.polyglot :as polyglot]
            [net.coruscation.js4clj.context :refer [*context* default-builder]]
            [net.coruscation.js4clj.core :as core]
            [net.coruscation.js4clj.js :as js]
            [net.coruscation.js4clj.require :as require]
            [net.coruscation.js4clj.utils :refer [clj->js js->clj js-set! js. js.- js..]]))

(deftest require-test

  (testing "Generic Require Test"
    (require/require-cjs '["luxon" :as grt-lux])
    (is (resolve 'grt-lux/DateTime)))

  (testing "Requiring commonjs modules test"
    (require/require-cjs '["./node_modules/luxon/build/cjs-browser/luxon.js" :as lux]
                         '["./node_modules/luxon/build/cjs-browser/luxon.js" :as lux2])
    (is (resolve 'lux/DateTime))
    (is (resolve 'lux2/DateTime)))

  (testing "Requiring esm modules test"
    (require/require-js '["node_modules/luxon/build/es6/luxon.mjs" :as elux]
                        '["node_modules/luxon/build/es6/luxon.mjs" :as elux2])
    (is (resolve 'elux/DateTime))
    (is (resolve 'elux2/DateTime))
    (is (string? (js.. (var-get (resolve 'elux/DateTime)) now toString))))

  (testing "Testing erronous require"
    (is (thrown?
         Throwable
         (require/require-cjs '[luxon :as luxon])))
    (is (thrown?
         Throwable
         (require/require-cjs '["luxon" :as "luxon"])))))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= (:as (#'require/parse-flags '[:as name]))
           'name))
    (is (:load-ns (#'require/parse-flags '[:load-ns :as name])))
    (is (= (:as (#'require/parse-flags '[:load-ns :as name]))
           'name))
    (is (let [{:keys [f1 f2 f3 f4 load-ns as]} (#'require/parse-flags '[:f1 :f2 :load-ns :as name :f3 :f4])]
          (and f1 f2 f3 f4 load-ns (= as 'name))))))

(require/require-cjs '["luxon" :as lux])

(deftest js-test
  (testing ""
    (let [test-dt (js. lux/DateTime fromISO "2017-05-15T08:30:00Z")]
      (is (= (js.- test-dt year)
             2017)))))

(deftest wrapper-method-test
  (testing ""
    ;; (is (function? js/Array))
    (is (= (polyglot/get-meta-qualified-name (polyglot/get-meta-object js/Array))
           "Function"))
    (is (= (polyglot/get-meta-qualified-name (polyglot/get-meta-object (polyglot/get-member js/Array "from")))
           "Function"))))

;; (deftest null-undefined-test
;;   (testing ""
;;     (is (js/js-undefined? js/undefined))
;;     (is (false? (js/js-undefined? nil)))
;;     (is (false? (js/js-undefined? (clj->js nil))))))

(deftest polyglot-value?-test
  (testing ""
    (is (true? (symbol? js/Array)) "js/Array is just a symbol now")
    (is (instance? org.graalvm.polyglot.Value (polyglot/polyglot-value js/Array) ) "We turn it into a polyglot value using polyglot-value")
    (is (true? (fn? (converting/clojurify-value (polyglot/polyglot-value js/Array)))) "We can't use it as a function now, maybe we need to solve this problem in the future")))

(deftest special-js-namespace-test
  (testing ""
    (is (symbol? js/Array)
        "It is just a symbol")
    (is (= (js->clj (core/js-new js/Array 1 2 3))
           [1 2 3])
        "We can use it as the class")
    (is (= (js->clj (core/js-new js/Array js/undefined js/undefined))
           [nil nil])
        "We can pass it as arguments")
    (is (true? (core/js-fn? js/Array))
        "functions starting with js- will treat these symbols as js values")))

(deftest js-array?-test
  (testing ""
    (is (true? (core/js-array? (clj->js [1 2 3]))) "it is a js array")
    (is (false? (core/js-array? nil)) "returns false, no error threw")
    (is (false? (core/js-array? js/undefined)) "returns false, no error threw")))

(deftest js->clj-test

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
      (is (= 2 (js.. obj -level1 -level2 -key)))))

  (testing "set nonexistent field"
    (let [obj (clj->js {:a 1})]
      (js-set! (js.- obj b)
               2)
      (is (= 2
             (js.- obj b)))))

  (testing "setting on not obj"
    (is (thrown-with-msg?
		 UnsupportedOperationException
         #"^Unsupported operation Value.putMember\(String, Object\) for 'undefined'\(language: JavaScript, type: undefined\).*$"
         (js-set! (js.- (.eval @*context* "js" "undefined") a) 1))))

  #_(testing "testing erroneous arguments"
      (is (thrown? Exception
                   (macroexpand '(js-set! a 1))))
      (is (thrown? Exception
                   (macroexpand '(js-set! (js. obj mtd) 1))))))

(deftest js-new-test
  (testing ""
    (is (= (js->clj (core/js-new js/Array 1 2 3 4))
           [1 2 3 4]))))

(deftest dot-macros-test
  (testing "Testing js.-"
    (is (=
         1
         (js.- (.eval @*context* "js"
                      "({a: 1})")
               "a")))
    (is (nil?
         (js.- (.eval @*context* "js" "({})")
               a))))

  (testing "Testing js."
    (is (= (-> (js. (js.- (js.- js/Array prototype) map)
                    call
                    (.eval @*context* "js" "[1,2,3]")
                    (fn [x & _]
                      (+ x 1)))
               js->clj)
           [2 3 4]))

    (is (thrown-with-msg?
         UnsupportedOperationException
         #"^Non readable or non-existent member key 'nonexist' for object.*"
         (js. js/Array nonexist))))

  (testing "Testing js.."
    ;; JavaScript String as represented as TruffleString by GraalVM
    ;;   other than a regular JavaScript object
    ;; So we can't use properties of a JavaScript string
    (is (thrown?
         UnsupportedOperationException
         (js.. lux/DateTime
               now
               toString
               -length)))))

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
           6))

    (let [double (fn [x] (* 2 x))
          js-identity (.eval @*context* "js" "(function (x) {return x;})")
          js-identity-wrapped (converting/clojurify-value js-identity)]
      (is (= double (js-identity-wrapped double)))))

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
    (is (not (function? js/Array)) "We can not use it as a Clojure function now")
    (is (core/js-fn? js/Array) "It is also a javascript function")
    (is (polyglot/can-instantiate js/Array)  "We can also instantiate it")
    (is (polyglot/get-member js/Array "from") "We can get its static method")
    (is (= (js->clj (js. js/Array from (clj->js [1 2 3])))
           [1 2 3]) "We can also call its static method")))


(deftest multithread-test
  (testing ""
    (try (let [future1 (future
                         (dotimes [i 1000]
                           (clj->js {:a "1"})))
               future2 (future
                         (dotimes [i 1000]
                           (clj->js {:a "1"})))]
           (list @future1 @future2))
         (catch java.util.concurrent.ExecutionException e
           (is (instance? java.lang.IllegalStateException
                          (.getCause e))))))

  ;; TODO: require-js is not compatible with multithreading usage
  (testing ""
    (let [future1 (future
                    (binding [*context*
                              (atom (.build (default-builder)))]
                      (dotimes [i 1000]
                        (clj->js {:a "1"})))
                    true)
          future2 (future
                    (binding [*context*
                              (atom (.build (default-builder)))]
                      (dotimes [i 1000]
                        (clj->js {:a "1"})))
                    true)]
      (is (= (list @future1 @future2)
             [true true])))))
