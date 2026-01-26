(ns net.coruscation.js4clj.api.converting-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [net.coruscation.js4clj.api.converting :as subject]
   [net.coruscation.js4clj.context :as context]
   [net.coruscation.js4clj.test-utils :refer [fresh-context]]))

(deftest js-undefined-raw?-test
  (is (true?
       (subject/js-undefined-raw?
        (.eval @context/*context* "js" "undefined"))))
  (is (false?
       (subject/js-undefined-raw?
        (.eval @context/*context* "js" "null"))))
  (is (false?
       (subject/js-undefined-raw?
        (.eval @context/*context* "js" "[]")))))

(use-fixtures :each #'fresh-context)
