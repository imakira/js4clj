(ns js4clj.js
  (:require [js4clj.context :refer [*context*]]
            [js4clj.core :refer [clojurify-value]])
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context Source Value]))

(def ^:dynamic *no-clojurify* false)

(defn- define-builtin [ns primitive & [alias]]
  (intern ns (if alias (symbol alias) (symbol primitive))
          ((if *no-clojurify* identity clojurify-value)
           (.getMember (.getBindings *context* "js") primitive))))

(defmacro define-builtins
  {:clj-kondo/lint-as 'clojure.core/declare}
  [ns & primitives]
  (create-ns ns)
  `(doseq [primitive# '~primitives]
     (define-builtin '~ns (str primitive#))))

;; In cljs there is also a js/undefined
;;	in which (= nil js/undefined) but we can't mimic it.
;; Still, we need a js/undefined in case we need to do some
;; 	very specific interop.
(declare undefined)
(with-bindings {#'*no-clojurify* true}
  (define-builtin 'js4clj.js "undefined" "undefined"))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(define-builtins js4clj.js
  globalThis
  Infinity
  NaN
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
  ((if *no-clojurify* identity
       clojurify-value)
   (.getMember (.getBindings *context* "js") name)))
