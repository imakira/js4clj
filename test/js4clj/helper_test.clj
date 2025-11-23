(ns js4clj.helper-test
  #_{:clj-kondo/ignore [:refer-all]}
  (:require
   [clojure.test :refer :all]
   [js4clj.api.converting :as core]))

(deftest core-test
  (testing ""
    (is (= (#'core/to-camel-style
            "get-member")
           "getMember"))
    (is (= (#'core/to-camel-style
            "get-meta-object")
           "getMetaObject"))
    (is (= (#'core/to-camel-style
            "has-meta-parents")
           "hasMetaParents"))
    (is (= (#'core/to-camel-style
            "get-meta-parents")
           "getMetaParents"))))
