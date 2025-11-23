(ns js4clj.core
  (:require [js4clj.api.polyglot :refer :all]
            [js4clj.api.converting :refer :all]))


(defn js-fn?
  "Check if a JavaScript value is a JavaScript function"
  [obj]
  (and (polyglot-value obj)
       (get-meta-object obj)
       (= (get-meta-qualified-name (get-meta-object obj))
          "Function")))

(defn js-array?
  "Check if a JavaScript value is a JavaScript array"
  [obj]
  (boolean (and
            (polyglot-value obj)
            (some-> obj
                    get-meta-object
                    get-meta-qualified-name
                    (= "Array")))))

(defn js-new [obj & args]
  "Create a new object of class `class` with arguments in `args`

  Example (js-new js/Array 1 2 3) => [1 2 3]"
  (apply new-instance obj (map polyglotalize-clojure args)))
