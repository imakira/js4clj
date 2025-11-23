(ns js4clj.api.converting
  (:require [js4clj.context :refer [*context*]]
            [js4clj.api.polyglot :refer :all])
  (:import (org.graalvm.polyglot Value Context)))

(declare clojurify-value)
(declare polyglotalize-clojure)

(defn wrap-clojure-fn [f]
  (.asValue *context*
            (with-raw-clojure-value
              (reify org.graalvm.polyglot.proxy.ProxyExecutable
                (execute [_ ^"[Lorg.graalvm.polyglot.Value;" values]
                  (polyglotalize-clojure (apply f (map clojurify-value values)))))
              f)))

(defn polyglotalize-clojure [value]
  (cond (get-raw-polyglot-value value)
        (get-raw-polyglot-value value)

        (fn? value)
        (wrap-clojure-fn value)

        :else
        ;; https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html#asValue(java.lang.Object)
        (.asValue *context* value)))

(defn wrap-polyglot-executable [^org.graalvm.polyglot.Value obj]
  (with-raw-polyglot-value
    (fn [& args]
      (clojurify-value (.execute obj (into-array Object (map polyglotalize-clojure args)))))
    obj))


(defn clojurify? [^org.graalvm.polyglot.Value value]
  (or (.isBoolean value)
      (.isNull value)
      (.isString value)
      (.isNumber value)
      (.canExecute value)
      (.isHostObject value)
      (.isProxyObject value)))

(defn clojurify-value [^org.graalvm.polyglot.Value value]
  (cond (.isProxyObject value)
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
