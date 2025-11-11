(ns js4clj.helper-test
  #_{:clj-kondo/ignore [:refer-all]}
  (:require  [clojure.test :refer :all]
             [js4clj.require :as require]))


(deftest core-test
  (testing ""
    (is (= (#'js4clj.core/to-camel-style
            "get-member")
           "getMember"))
    (is (= (#'js4clj.core/to-camel-style
            "get-meta-object")
           "getMetaObject"))
    (is (= (#'js4clj.core/to-camel-style
            "has-meta-parents")
           "hasMetaParents"))
    (is (= (#'js4clj.core/to-camel-style
            "get-meta-parents")
           "getMetaParents"))))

(deftest require-test
  (testing "in require.clj"
    (is (= (#'require/normalize-module-name "js4clj.modules.react-dom/cjs/react-dom-server-legacy.browser.development")
           "js4clj.modules.react-dom.cjs.react-dom-server-legacy.browser.development"))))
