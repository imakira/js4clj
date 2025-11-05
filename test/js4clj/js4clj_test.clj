(ns js4clj.js4clj-test
  (:require [clojure.test :refer :all]
            [js4clj.js4clj :as js4clj :refer [js. js.. js.- require-js]]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= (:as (#'js4clj/parse-flags '[:as name]))
           'name))
    (is (:load-ns (#'js4clj/parse-flags '[:load-ns :as name])))
    (is (= (:as (#'js4clj/parse-flags '[:load-ns :as name]))
           'name))
    (is (let [{:keys [f1 f2 f3 f4 load-ns as]} (#'js4clj/parse-flags '[:f1 :f2 :load-ns :as name :f3 :f4])]
          (and f1 f2 f3 f4 load-ns (= as 'name))))))

(require-js '[luxon :as lux])

(deftest js-test
  (testing ""
    (let [test-dt (js. lux/DateTime fromISO "2017-05-15T08:30:00Z")]
      (is (= (js.- test-dt year)
             2017)))))

(deftest clj->js-test
  (let [dt (js. lux/DateTime fromObject
             (js4clj/clj->js {:year 2012 :day 22 :hour 12})
             (js4clj/clj->js {:zone "America/Los_Angeles"
                              :numberingSystem "beng"}))]
    (is (= (:year (js4clj/js->clj dt :keywordize-keys true))
           2012))
    (is (= (:year (js4clj/js->clj dt :keywordize-keys true))
           2012))))
