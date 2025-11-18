(ns js4clj.helper-test
  #_{:clj-kondo/ignore [:refer-all]}
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [js4clj.require :as require]
   [js4clj.api.utils :refer [str->path]])
  (:import
   [java.nio.file Path]))


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
           "js4clj.modules.react-dom.cjs.react-dom-server-legacy.browser.development"))
    (is (.endsWith (.toPath (io/file (:directory (#'require/find-package-json "."))))
                   "js4clj"))
    (is (= (get (:content (#'require/find-package-json "."))
                "name")
           "js4clj"))
    (is (= (get (:content (#'require/find-package-json "node_modules/luxon"))
                "name")
           "luxon"))
    (is (= (#'require/find-package-json "/")
           nil))

    (is (= (#'require/find-package-json "/non/existence/432890")
           nil)))

  (testing "testing find-module"
    (let [pwd (.toAbsolutePath (str->path ""))]
      (is (= (.toString
              (.relativize pwd
                           (str->path (:module-location (#'require/find-module "luxon")))))
             "node_modules/luxon/build/es6/luxon.mjs"))))

  (testing "testing utils related to exports field in package.json"
    (is (= (#'require/wildcard-apply-matches "/abc*/bc*.js" ["dir" "main"])
           "/abcdir/bcmain.js"))
    (is (= (#'require/select-result-in-exports  "main.js")
           "main.js"))
    (is (= (#'require/select-result-in-exports {"node" "./feature-node.js"
                                                "default" "./feature.js"
                                                "browser" "./feature-browser.js"})
           "./feature.js"))
    (is (= (#'require/select-result-in-exports {"node" "./feature-node.js"
                                                "default" "./feature.js"
                                                "browser" "./feature-browser.js"})
           "./feature.js"))
    (is (= (#'require/select-result-in-exports {"node" "./feature-node.js"
                                                "browser" "./feature-browser.js"})
           "./feature-browser.js"))
    (is (= (#'require/select-result-in-exports {"node" "./feature-node.js"})
           "./feature-node.js"))

    (is (= "main.js"
           (#'require/find-module-with-exports
            (Path/of "" (into-array String []))
            "main.js")))

    (is (= "main.js"
           (#'require/find-module-with-exports
            (Path/of "" (into-array String []))
            {"." "main.js"})))

    (is (= "main.js"
           (#'require/find-module-with-exports
            (Path/of "" (into-array String []))
            {"." {"default" "main.js"}})))

    (is (= "main-browser.js"
           (#'require/find-module-with-exports
            (Path/of "" (into-array String []))
            {"." {"browser" "main-browser.js"
                  "node" "main-node.js"}})))

    (is (= "./lib/index.js"
           (#'require/find-module-with-exports
            (Path/of "./lib/index.js" (into-array String []))
            {"." "./lib/index.js"
             "./lib/index" "./lib/index.js"
             "./lib/index.js" "./lib/index.js"})))

    (is (= "./lib/index.js"
           (#'require/find-module-with-exports
            (Path/of "./lib/index.js" (into-array String []))
            {"./lib/*" "./lib/*.js"
             "./lib/*.js" "./lib/index.js"})))

    (is (= "./lib/index.js"
           (#'require/find-module-with-exports
            (Path/of "./lib/index.js" (into-array String []))
            {"./lib/*" {"default" nil}
             "./lib/*.js" {"default" "./lib/index.js"}})))

    (is (= "./map/comp/index.js"
           (#'require/find-module-with-exports
            (Path/of "./lib/comp/index.js" (into-array String []))
            {"./lib/*/*.js" {"default" "./map/*/*.js"}})))))
