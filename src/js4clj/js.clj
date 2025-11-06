(ns js4clj.js
  (:require [js4clj.context :refer [*context*]]
            [js4clj.core :refer [clojurify-value]])
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context Source Value]))

(defn- define-primitive [ns primitive]
  (intern ns (symbol primitive)
          (clojurify-value (.eval *context* "js" primitive))))

(defmacro define-primitives
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [ns & primitives]
  (create-ns ns)
  `(doseq [primitive# '~primitives]
     (define-primitive '~ns (str primitive#))))

(define-primitives js4clj.js
  globalThis
  Infinity
  NaN
  undefined
  null
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
  Array
  Map
  Set
  WeakMap
  WeakSet
  JSON
  ArrayBuffer
  Promise
  console)



