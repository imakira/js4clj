(ns js4clj.js
  (:require [js4clj.context :refer [*context*]]
            [js4clj.api.converting :refer [clojurify-value]]
            [js4clj.api.polyglot :refer :all])
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context Source Value]))

(def ^{:dynamic true :private true} *no-clojurify* false)

(defn- define-builtin [ns primitive & [alias]]
  (intern ns (if alias (symbol alias) (symbol primitive))
          ((if *no-clojurify* identity clojurify-value)
           (.getMember (.getBindings *context* "js") primitive))))

(defmacro ^:private define-builtins
  {:clj-kondo/lint-as 'clojure.core/declare}
  [ns & primitives]
  (create-ns ns)
  `(doseq [primitive# '~primitives]
     (define-builtin '~ns (str primitive#))))

;; In cljs there is also a js/undefined
;;	in which (= nil js/undefined) but we can't mimic it.
;; Still, we need a js/undefined in case we need to do some
;; 	very specific interop.
(def undefined
  "Internal representation of `undefined` in JavaScript.

  Only use this when you want to pass a undefined value to a JavaScript function.

  Unlike ClojureScript, this value does not equal to `nil`"
  (.getMember (.getBindings *context* "js") "undefined"))

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

(defn builtin
  "Get the top-level JavaScript value of `name`"
  [name]
  ((if *no-clojurify* identity
       clojurify-value)
   (.getMember (.getBindings *context* "js") name)))


(defn js-undefined?
  "Check if a JavaScript value is undefined"
  [obj]
  (and (instance? org.graalvm.polyglot.Value obj)
       (boolean
        (some-> obj
                get-meta-object
                get-meta-qualified-name
                (= "undefined")))))
