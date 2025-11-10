(ns js4clj.helper-test
  #_{:clj-kondo/ignore [:refer-all]}
  (:require  [clojure.test :refer :all]))


(deftest helper-test
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
