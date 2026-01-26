(ns net.coruscation.js4clj.context
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context])
  (:require [clojure.java.io :as io]))

(def ^{:dynamic true} *js-cwd*
  "cwd of the javascript environment when doing `require` or `import`.
  In most cases it is the directory contains `node_modules`

  Default: cwd of the Clojure process"
  (System/getProperty "user.dir"))

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
  (let [cwd-dir (io/file *js-cwd*)]
    (when (not (.exists cwd-dir))
      (.mkdirs cwd-dir)))
  (-> (Context/newBuilder (into-array String ["js"]))
      (.allowExperimentalOptions true)
      (.options (HashMap.
                 (merge {"js.esm-eval-returns-exports" "true"}
                        (if *js-cwd*
                          {"js.commonjs-require" "true"
                           "js.commonjs-require-cwd" *js-cwd*}
                          {}))))
      (.allowIO true)))

(def ^:dynamic *context-per-thread*
  "Whether to use thread local context when doing js4clj operations."
  false)

(def ^:dynamic *customize-builder*
  "A fn takes a `Context$Builder` as an argument and returns a `Context$Builder`"
  nil)

(def ^:private builtin-after-init
  (fn [ctx]
    (.eval ctx "js" "globalThis.global = this")
    (.eval ctx "js" "globalThis.process = {}")
    (.eval ctx "js" "globalThis.process.env = {}")))

(def ^:dynamic *after-init-hook*
  "A fn takes no arguments, called after a context is created, with *context* bound to the newly created context."
  nil)

(declare *context*)

(defn context-new
  "Create a context, respect `customize-builder` and `after-init-hook`"
  []
  (let [context (-> (default-builder)
                    (or *customize-builder* identity)
                    (.build))]
    (when builtin-after-init (builtin-after-init context))
    (when *after-init-hook*
      (binding [*context* (ref context)]
        (*after-init-hook*)))
    context))

(def ^:private global-context (atom nil))

(def ^:private thread-local-context (ThreadLocal/withInitial (fn [] (context-new))))

(def ^{:dynamic true
       :doc
       "When `*context-per-thread*` is false, deref it returns the global context,
        When `*context-per-thread*` is true, deref it returns the thread local context.

	    In any case, if it is not already initialized, initialize it with `context-new`

	    You can bind it with an IDeref instance to manage context."}
  *context* (reify clojure.lang.IDeref
              (deref [_]
                (if *context-per-thread*
                  (.get thread-local-context)
                  (if (not (nil? @global-context))
                    @global-context
                    (swap! global-context
                           (fn [old]
                             (if old
                               old
                               (context-new)))))))))

(defn reinitialize-context!
  "If `*context-per-thread*` is false, reinitialize the global `*context*` with `context-new`.
  If `*context-per-thread*` is true, reinitialize the thread-local context with `context-new`.

  Note: Contexts are automatically initialize upon first usage  if not already initialized.
  "
  []
  (if *context-per-thread*
    (.set thread-local-context (context-new))
    (reset! global-context (context-new))))
