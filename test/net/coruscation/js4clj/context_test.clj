(ns net.coruscation.js4clj.context-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [net.coruscation.js4clj.context :as subject]
   [net.coruscation.js4clj.js :as js]
   [net.coruscation.js4clj.test-utils :refer [fresh-context]]
   [net.coruscation.js4clj.utils :refer [js-set! js.-]]))

(deftest *context-per-thread*-test
  (binding [subject/*context-per-thread* true]
    (.eval @subject/*context*
           "js"
           "globalThis.demo = 1")
    (let [thread (Thread/new
                  (bound-fn []
                    (is (nil? (js.- js/globalThis
                                    demo)))))]
      (.start thread)
      (is (= 1
             (js.- js/globalThis
                   demo)))
      (.join thread
             200))))

(deftest *after-init-hook*-test
  (binding [subject/*after-init-hook*
            (fn []
              (js-set! (js.- js/globalThis
                             demo)
                       2))]
    (subject/reinitialize-context!)
    (is (= 2 (js.- js/globalThis
                   demo)))))

(deftest reinitialize-context!-test
  (is (nil? (js.- js/globalThis
                  demo)))
  (js-set! (js.- js/globalThis
                 demo)
           2)
  (is (= 2
         (js.- js/globalThis
               demo)))
  (subject/reinitialize-context!)
  (is (nil? (js.- js/globalThis
                  demo))))

(use-fixtures :each #'fresh-context)
