(ns net.coruscation.js4clj.context
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context]))

(defn default-builder
  "Return a `org.graalvm.polyglot.Context$Builder` object with necessary options set for js4clj to function properly.

  You can build more JS context with it, then dynamic binding *context* variable to use multiple JS contexts in
	multiple threads. For example:

  ```clojure
  (binding [*context* (atom (.build (default-builder)))]
    (future ...))
  ```

  "
  []
  (-> (Context/newBuilder (into-array String ["js"]))
      (.allowExperimentalOptions true)
      (.options (HashMap.
                 {"js.esm-eval-returns-exports" "true"
                  "js.commonjs-require" "true"
                  "js.commonjs-require-cwd" (str (System/getProperty "user.dir")
                                                 "/node_modules")}))
      (.allowIO true)))

(defonce ^{:dynamic true
           :doc "An atom wrapping a instance of `org.graalvm.polyglot.Context$Builder`, used for all the operations "}
  *context* (atom (.build (default-builder))))

(defn initialize-context!
  "Initialize the `*context*` variable with a lambda taking `default-builder` as an argument.

  `defaut-builder` is an `org.graalvm.polyglot.Context$Builder` object with necessary option set for js4clj to function properly.
  The lambda should return a `org.graalvm.polyglot.Context` object to initialize the *context* variable.

  Example:
  ```clojure
  (initialize-context! (fn [builder]
    (.build builder)))
  ```
  "
  [build-fn]
  (let [default-builder (default-builder)]
    (reset! *context* (build-fn default-builder))))

(.eval @*context* "js" "globalThis.global = this")
(.eval @*context* "js" "globalThis.process = {}")
(.eval @*context* "js" "globalThis.process.env = {}")
