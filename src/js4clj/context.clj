(ns js4clj.context 
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context]))

(defonce ^:dynamic *context*
  (-> (Context/newBuilder (into-array String ["js"]))
      (.allowExperimentalOptions true)
      (.options (HashMap.
                 {"js.esm-eval-returns-exports" "true"
                  "js.commonjs-require" "true"
                  "js.commonjs-require-cwd" (str (System/getProperty "user.dir")
                                                 "/node_modules")}))
      (.allowIO true)
      (.build)))

(.eval *context* "js" "globalThis.global = this")
(.eval *context* "js" "globalThis.process = {}")
(.eval *context* "js" "globalThis.process.env = {}")
