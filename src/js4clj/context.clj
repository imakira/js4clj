(ns js4clj.context 
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context]))

(defonce ^:dynamic *context*
  (-> (Context/newBuilder (into-array String ["js"]))
      (.allowExperimentalOptions true)
      (.options (HashMap.
                 {"js.commonjs-require" "true"
                  "js.commonjs-require-cwd" (str (System/getProperty "user.dir")
                                                 "/node_modules")}))
      (.allowIO true)
      (.build)))
