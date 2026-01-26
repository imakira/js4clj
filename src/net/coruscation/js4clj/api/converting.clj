(ns net.coruscation.js4clj.api.converting
  (:require [net.coruscation.js4clj.context :refer [*context*]]
            [net.coruscation.js4clj.api.polyglot :refer :all])
  (:import (org.graalvm.polyglot Value Context)))

(declare clojurify-value)
(declare polyglotalize-clojure)

(defn wrap-clojure-fn
  "Wrap a Clojure fn as a Polyglot Executable.
   Retrieve the original clojure fn back using `(polyglot/get-raw-clojure-value (.asProxyObject value))`"
  [f]
  (.asValue @*context*
            (with-raw-clojure-value
              (reify org.graalvm.polyglot.proxy.ProxyExecutable
                (execute [_ ^"[Lorg.graalvm.polyglot.Value;" values]
                  (polyglotalize-clojure (apply f (map clojurify-value values)))))
              f)))

(defn polyglotalize-clojure [value]
  (cond (polyglot-value value)
        (polyglot-value value)

        (fn? value)
        (wrap-clojure-fn value)

        :else
        ;; https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html#asValue(java.lang.Object)
        (.asValue @*context* value)))

(defn wrap-polyglot-executable
  "Wrap a Polyglot Value as a Clojure fn, so that they can be easily used from the Clojure side.
   The returned fn has metadata created using `polyglot/with-raw-polyglot-value`, and you can retrieve the original Polyglot Value from the fn using `polyglot/polyglot-value`"
  [^org.graalvm.polyglot.Value obj]
  (with-raw-polyglot-value
    (fn [& args]
      (clojurify-value (.execute obj (into-array Object (map polyglotalize-clojure args)))))
    obj))


(defn clojurify?
  "Check if a Polyglot Value can be converted into a Clojure value."
  [^org.graalvm.polyglot.Value value]
  (or (.isBoolean value)
      (.isNull value)
      (.isString value)
      (.isNumber value)
      (.canExecute value)
      (.isHostObject value)
      (.isProxyObject value)))

(defn clojurify-value [^org.graalvm.polyglot.Value value]
  (cond (nil? value)
        nil

        (.isProxyObject value)
        (let [prox-obj (.asProxyObject value)]
          (if (get-raw-clojure-value prox-obj)
            (get-raw-clojure-value prox-obj)
            prox-obj))

        (.isHostObject value)
        (.asHostObject value)

        (.isBoolean value)
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
        ;; TODO
        ;; polyglot time, date, time zone, instant, duration
        :else
        value))

(defn js-undefined-raw?
  "Check if an value is an instance of `org.graalvm.polyglot.Value` and of JavaScript `undefined` value"
  [value]
  (and (instance? org.graalvm.polyglot.Value value)
       (= (some-> value
                  (.getMetaObject)
                  (.getMetaQualifiedName))
          "undefined")))
