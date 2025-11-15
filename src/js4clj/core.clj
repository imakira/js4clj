(ns js4clj.core
  (:require [js4clj.context :refer [*context*]]
            [clojure.string :as string])
  (:import (org.graalvm.polyglot Value Context)))

(declare clojurify-value)
(declare polyglotalize-clojure)

;;TODO what about /this/
(defn wrap-clojure-fn [f]
  (.asValue *context*
            (with-meta (reify org.graalvm.polyglot.proxy.ProxyExecutable
                         #_{:clj-kondo/ignore [:unused-binding]}
                         (execute [this ^"[Lorg.graalvm.polyglot.Value;" values]
                           (polyglotalize-clojure (apply f (map clojurify-value values)))))
              {::raw-fn f})))

(defn polyglotalize-clojure [value]
  (cond (::raw-value (meta value))
        (::raw-value (meta value))

        (fn? value)
        (wrap-clojure-fn value)

        :else
        ;; https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html#asValue(java.lang.Object)
        (.asValue *context* value)))

(defn- to-camel-style [s]
  (string/replace s #"-([a-z])" (fn [g]
                                  (.toUpperCase (second g)))))


(defn polyglot-value [obj]
  (or (::raw-value (meta obj))
      (and (instance? org.graalvm.polyglot.Value obj)
           obj)))

(defmacro define-unwrap-executable-alias
  {:clj-kondo/lint-as 'clojure.core/declare}
  [name & args]
  (let [docstring? (string? (first args))
        docstring (when docstring? (first args))
        args (if docstring? (second args) (first args))
        obj 'obj
        [arglist [_ vararg]] (split-with (fn [x] (not (= x '&))) args)] 
    `(defn ~name {:doc ~docstring} [~obj ~@arglist ~@(if vararg `[& ~vararg] [])]
       (let [~obj (or (polyglot-value ~obj)
                      ~obj)]
         (~(symbol (str "." (to-camel-style (str name))))
          ~obj 
          ~@arglist
          ~@(if vararg
              [`(into-array Object ~vararg)]
              []))))))

(define-unwrap-executable-alias get-meta-object)
(define-unwrap-executable-alias is-meta-object)
(define-unwrap-executable-alias get-meta-qualified-name)
(define-unwrap-executable-alias has-meta-parents)
(define-unwrap-executable-alias has-array-elements)
(define-unwrap-executable-alias get-meta-parents)
(define-unwrap-executable-alias get-member [^String identifier])
(define-unwrap-executable-alias put-member [^String identifier ^Object value])
(define-unwrap-executable-alias get-member-keys)
(define-unwrap-executable-alias new-instance [& args])
(define-unwrap-executable-alias can-invoke-member [^String s])
(define-unwrap-executable-alias invoke-member [^String method & args])
(define-unwrap-executable-alias can-instantiate)
(define-unwrap-executable-alias canExecute)
(define-unwrap-executable-alias execute [& args])
(define-unwrap-executable-alias executeVoid [& args])

(defn wrap-polyglot-executable [^org.graalvm.polyglot.Value obj]
  (with-meta (fn [& args]
               (clojurify-value (.execute obj (into-array Object (map polyglotalize-clojure args)))))
    {::raw-value obj}))

(defn js-fn? [obj]
  (and (get-meta-object obj)
       (= (get-meta-qualified-name (get-meta-object obj))
          "Function")))

(defn js-array? [obj]
  (boolean (and
            (polyglot-value obj)
            (some-> obj
                    get-meta-object
                    get-meta-qualified-name
                    (= "Array")))))

(defn js-undefined? [obj]
  (and (instance? org.graalvm.polyglot.Value obj)
       (boolean
        (some-> obj
                get-meta-object
                get-meta-qualified-name
                (= "undefined")))))

(defn clojurify? [^org.graalvm.polyglot.Value value]
  (or (.isBoolean value)
      (.isNull value)
      (.isString value)
      (.isNumber value)
      (.canExecute value)
      (.isHostObject value)
      (.isProxyObject value)))

(defn clojurify-value [^org.graalvm.polyglot.Value value]
  (cond (.isBoolean value)
        (.asBoolean value)

        (.isNull value)
        nil

        (.isString value)
        (.asString value)

        (.isNumber value)
        (cond (.fitsInLong value)
              (.asLong value)

              (.fitsInDouble value)
              (.asDouble value))

        (.canExecute value) 
        (wrap-polyglot-executable value)

        (.isHostObject value)
        (.asHostObject value)

        (.isProxyObject value)
        (let [prox-obj (.asProxyObject value)]
          (if (::raw-fn (meta prox-obj))
            (::raw-fn (meta prox-obj))
            prox-obj))

        ;; TODO
        ;; polyglot time, date, time zone, instant, duration
        :else
        value))

(defn instantiate [obj & args]
  (apply new-instance obj (map polyglotalize-clojure args)))

