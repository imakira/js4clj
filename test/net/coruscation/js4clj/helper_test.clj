(ns net.coruscation.js4clj.helper-test
  #_{:clj-kondo/ignore [:refer-all]}
  (:require
   [clojure.test :refer :all]
   [net.coruscation.js4clj.api.polyglot :as polyglot]))

(deftest core-test
  (testing ""
    (is (= (#'polyglot/to-camel-style
            "get-member")
           "getMember"))
    (is (= (#'polyglot/to-camel-style
            "get-meta-object")
           "getMetaObject"))
    (is (= (#'polyglot/to-camel-style
            "has-meta-parents")
           "hasMetaParents"))
    (is (= (#'polyglot/to-camel-style
            "get-meta-parents")
           "getMetaParents"))))
