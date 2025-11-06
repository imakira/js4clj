(ns js4clj.context 
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context]))

(def ^:dynamic *context*
  (-> (Context/newBuilder (into-array String ["js"]))
      (.allowExperimentalOptions true)
      (.options (HashMap.
                 {"js.commonjs-require" "true"
                  "js.commonjs-require-cwd" (str (System/getProperty "user.dir")
                                                 "/node_modules")
                  "js.commonjs-core-modules-replacements" "crypto:crypto-browserify,stream:stream-browserify"}))
      (.allowIO true)
      (.build)))

(.eval *context* "js" "globalThis.global = this")
(.eval *context* "js" "globalThis.process = {}")
(.eval *context* "js" "globalThis.process.env = {'NODE_ENV': 'production'}")
