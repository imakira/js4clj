(ns net.coruscation.js4clj.utils
  (:require [net.coruscation.js4clj.api.converting :refer :all]
            [net.coruscation.js4clj.api.polyglot :refer :all]
            [net.coruscation.js4clj.core :refer :all]
            [net.coruscation.js4clj.js :as js :refer [builtin]]))

#_{:clj-kondo/ignore [:syntax]}
(defn- js-call-helper [obj method & args]
  `(apply invoke-member ~obj ~(str method)
          ;; evaluate args before passing  to polyglotalize-clojure
          (map polyglotalize-clojure (list ~@args))))

#_{:clj-kondo/ignore [:syntax]}
(defmacro js.
  "(js. object method) => object.method()

  Invoke `method` of the object `object`, like the builtin `.`"
  ;; TODO
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [obj method & args]
  `(clojurify-value
    ~(apply js-call-helper obj method args)))

(defn- js-get-helper [obj field]
  `(get-member ~obj ~(str field)))

(defmacro js.-
  "(js.- object field) => object.field

  Access `field` of the object `object`, like the builtin `.-`
  "
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [obj field]
  `(clojurify-value ~(js-get-helper obj field)))

(defn- js-chan-access-helper [obj & args]
  (if (empty? args)
    obj
    (let [curr# (first args)
          rest# (rest args)]
      (apply js-chan-access-helper
             (cond (seq? curr#)
                   (apply js-call-helper obj (first curr#) (rest curr#))

                   (.startsWith (str curr#)
                                "-")
                   (js-get-helper obj (subs (str curr#) 1))
                   :else
                   (js-call-helper obj curr#))
             rest#))))

#_{:clj-kondo/ignore [:syntax]}
(defmacro js..
  "(js.. object -field1 -field2 method) => object.field1.field2.method()

  Chained field access or method invoking of `object`, like the builtin `..`"
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [obj & args]
  `(clojurify-value ~(apply js-chan-access-helper obj args)))

(defn- create-js-array [& args]
  (apply js-new (builtin "Array") args))

(defn- polyglot-iterable->vector [^org.graalvm.polyglot.Value obj & [f]]
  (assert (.hasIterator obj))
  (let [iter (.getIterator obj)]
    (persistent!
     (loop [result (transient [])]
       (if (.hasIteratorNextElement iter)
         (recur (conj! result ((or f identity)
                               (.getIteratorNextElement iter))))
         result)))))

(defn- polyglot-object->map [^org.graalvm.polyglot.Value obj & {:keys [key-fn value-fn]}]
  (assert (.hasMembers obj))
  (persistent!
   (loop [result (transient {})
          keys (into '() (.getMemberKeys obj))]
     (if (empty? keys)
       result
       (recur (assoc! result
                      ((or key-fn identity) (first keys))
                      ((or value-fn identity)
                       (.getMember obj (first keys))))
              (rest keys))))))

(defn clj->js
  "Convert a Clojure value to a JavaScript one

  `set`/`vector`/`list` -> JavaScript Array
  `keyword`/symbol -> String
  `map` -> JavaScript Object "
  [obj]
  (cond (or (set? obj)
            (vector? obj)
            (list? obj))
        (apply create-js-array (map clj->js obj))

        (keyword? obj)
        (subs (str obj)
              1)

        (symbol? obj)
        (str obj)

        (map? obj)
        (let [js-obj (js-new (builtin "Object"))]
          (doseq [[k v] obj]
            (.putMember js-obj (clj->js k) (clj->js v)))
          js-obj)

        :else
        (polyglotalize-clojure obj)))

(defn js->clj
  "Recursively convert a JavaScript object to a Clojure value

  With option {:keywordize-keys true} will convert object fields from strings to keywords.

  Converting Table:
  JavaScript Executable -> Clojure fn
  JavaScript Array -> Clojure Vector
  Any JavaScript Object not instantiable or executable -> Clojure Map"
  [value & {:keys [keywordize-keys] :or {keywordize-keys false}}]
  (if (not (isa? (class value) org.graalvm.polyglot.Value))
    ;; if it is not a js obj, return as is
    value
    (let [f (fn thisfn [^org.graalvm.polyglot.Value value]
              (cond
                ;; for: boolean null string number executable
                (clojurify? value)
                (clojurify-value value)

                (js-array? value)
                (polyglot-iterable->vector value thisfn)

                :else
                (polyglot-object->map value
                                      :key-fn (if keywordize-keys
                                                keyword
                                                identity)
                                      :value-fn thisfn)))]
      (f value))))

(defmacro js-set!
  "(js-set! (js.- obj field) value)
  (js-set! (js.. obj -field) value)

  Set the property of a JavaScript object to `value`, the property is accessed with `js.-` or `js..` in the first parameter"
  [dot-form value]
  (assert (seq? dot-form) "First argument must be in the form of `(js.. obj -field)` or (js.- obj field)")
  (let [[op & args] dot-form]
    (assert (or (= op 'js..)
                (= op 'js.-))
            "First argument to js-set! must start with either `js..` or `js.-`")
    (let [remove-hyphen (= op 'js..)
          lst (last args)
          last-removed (drop-last (into [op] args))]
      `(put-member ~(if (= (count last-removed) 2)
                      (second last-removed)
                      last-removed)
                   ~(if remove-hyphen
                      (subs (str lst) 1)
                      (str lst))
                   ~value))))


(defn js-select-keys
  "Returns a map containing only those entries in the JavaScript object whose key is in keys
  You can use strings or keywords as keys, and it will return a map using strings or keywords as keys correspondently"
  [obj keys]
  (into {} (for [key keys]
             (let [key-str (name key)]
               (if (has-member obj key-str)
                 [key (clojurify-value (get-member obj key-str))]
                 nil)))))

(defmacro select-keys-bind [keys obj & body]
  `(let [{:keys [~@keys]}
         (js-select-keys ~obj ~(mapv keyword keys))]
     ~@body))
