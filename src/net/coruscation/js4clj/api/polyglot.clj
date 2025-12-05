(ns net.coruscation.js4clj.api.polyglot
  (:require
   [clojure.string :as string]
   [net.coruscation.js4clj.context :refer [*context*]]))

(defn with-raw-clojure-value [polyglot-value clojure-value]
  (with-meta polyglot-value
    {::raw-clojure-value clojure-value}))

(defn get-raw-clojure-value [polyglot-value-with-metadata]
  (::raw-clojure-value (meta polyglot-value-with-metadata)))

(defn with-raw-polyglot-value [clojure-value polyglot-value]
  (with-meta clojure-value
    {::raw-polyglot-value polyglot-value}))

(defn polyglot-value [obj]
  (cond (::raw-polyglot-value (meta obj))
        (::raw-polyglot-value (meta obj))

        (and (symbol? obj)
             (= (namespace obj)
                (name 'net.coruscation.js4clj.js)))
        (.getMember (.getBindings @*context* "js")
                    (name obj))

        (instance? org.graalvm.polyglot.Value obj)
        obj

        :else
        nil))

(defn- to-camel-style [s]
  (string/replace s #"-([a-z])" (fn [g]
                                  (.toUpperCase (second g)))))

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
(define-unwrap-executable-alias get-source-location [])
