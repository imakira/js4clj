(ns js4clj.core
  (:require [js4clj.context :refer [*context*]]))

(declare clojurify-value)

;;TODO what about /this/
(defn wrap-clojure-fn [f]
  (reify org.graalvm.polyglot.proxy.ProxyExecutable
    #_{:clj-kondo/ignore [:unused-binding]}
    (execute [this ^"[Lorg.graalvm.polyglot.Value;" values]
      (apply f (map clojurify-value values)))))

(defn polyglotalize-clojure [value]
  (cond (fn? value)
        (wrap-clojure-fn value)

        :else
        (.asValue *context* value)))


(defn wrap-polyglot-execute [^org.graalvm.polyglot.Value obj]
  (fn [& args]
    (clojurify-value (.execute obj (into-array Object (map polyglotalize-clojure args))))))

(defn wrap-polyglot-invoke-member [^org.graalvm.polyglot.Value obj ^String method]
  (fn [& args]
    (clojurify-value (.invokeMember obj method (into-array Object (map polyglotalize-clojure args))))))

(defn polyglot-primitive-type? [^org.graalvm.polyglot.Value obj]
  (or (.isBoolean obj)
      (.isNull obj)
      (.isString obj)
      (.isNumber obj)))

(defn polyglot-primitive->-clj [^org.graalvm.polyglot.Value obj]
  (assert polyglot-primitive-type? obj)
  (cond (.isBoolean obj)
        (.asBoolean obj)

        (.isNull obj)
        nil

        (.isString obj)
        (.asString obj)

        (.isNumber obj)
        (cond (.fitsInLong obj)
              (.asLong obj)

              (.fitsInDouble obj)
              (.asDouble obj))))

(defn javascript-simple-function? [^org.graalvm.polyglot.Value obj]
  (and (and (.getMetaObject obj) (= (.getMetaQualifiedName (.getMetaObject obj))
                                    "Function"))
       (not (.canInstantiate obj))))

(defn array? [^org.graalvm.polyglot.Value obj]
  (= (.getMetaQualifiedName (.getMetaObject obj))
     "Array"))

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

        (javascript-simple-function? value)
        (wrap-polyglot-execute value)

        (.hasMembers value)
        value

        ;; TODO
        ;; polyglot time, date, time zone, instant, duration
        :else
        value))

(defn instantiate [obj & args]
  (.newInstance obj (into-array Object (map polyglotalize-clojure args))))

