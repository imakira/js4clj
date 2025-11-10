(ns js4clj.js
  (:require [js4clj.context :refer [*context*]]
            [js4clj.core :refer [clojurify-value]])
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context Source Value]))

(defn- define-builtin [ns primitive]
  (intern ns (symbol primitive)
          (clojurify-value (.eval *context* "js" primitive))))

(defmacro define-builtins
  {:clj-kondo/lint-as 'clojure.core/declare}
  [ns & primitives]
  (create-ns ns)
  `(doseq [primitive# '~primitives]
     (define-builtin '~ns (str primitive#))))

(define-builtins js4clj.js
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

(defn builtin [name]
  (clojurify-value (.eval *context* "js" name)))
