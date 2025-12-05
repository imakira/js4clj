(ns net.coruscation.js4clj.js
  (:require [net.coruscation.js4clj.context :refer [*context*]]
            [net.coruscation.js4clj.api.converting :refer [clojurify-value]]
            [net.coruscation.js4clj.api.polyglot :refer :all])
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context Source Value]))

(def ^{:dynamic true :private true} *no-clojurify* false)

(def ^:private nullprint (java.io.PrintWriter. (proxy [java.io.OutputStream] []
                                                 (write [_ _]))))
(defn- define-builtin [ns primitive]
  (binding [*err* nullprint]
    (intern ns (with-meta primitive
                 {:doc (str "A symbol stands for js `" (name primitive) "`")})
            (symbol (name ns) (name primitive)))))

(defmacro ^:private define-builtins
  {:clj-kondo/lint-as 'clojure.core/declare}
  [ns & primitives]
  (create-ns ns)
  `(doseq [primitive# '~primitives]
     (define-builtin '~ns primitive#)))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(define-builtins net.coruscation.js4clj.js
  globalThis
  Infinity
  NaN
  undefined
  Object
  Function
  Boolean
  Symbol
  Error
  Number
  BigInt
  Math
  Date
  String
  RegExp
  Array
  Int8Array
  Uint8Array
  Int16Array
  Int32Array
  Uint32Array
  BigInt64Array
  BigUint64Array
  Float16Array
  Float32Array
  Float64Array
  Map
  Set
  WeakMap
  WeakSet
  ArrayBuffer
  SharedArrayBuffer
  DataView
  Atomics
  JSON
  WeakRef
  FinalizationRegistry
  Promise
  Iterator
  Reflect
  Proxy
  Intl
  console)

(defn builtin
  "Get the top-level JavaScript value of `name`"
  [name]
  ((if *no-clojurify* identity
       clojurify-value)
   (.getMember (.getBindings @*context* "js") name)))
