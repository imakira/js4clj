(ns js4clj.js4clj
  (:require [js4clj.js :refer [*context*] :as js])
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context Source Value]))

(defn polyglotalize-clojure [value]
  (-> *context*
      (.asValue value)))

(declare clojurify-value)
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

(defn polyglot-primitive-to-clj [^org.graalvm.polyglot.Value obj]
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
  (and (= (.getMetaQualifiedName (.getMetaObject obj))
          "Function")
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


(defn- require-module [name]
  (let  [module (-> *context*
                    (.eval "js"
                           (str "require('" name "')")))]
    (assert (.hasMembers module)
            (str name " is not a module"))
    module))

(defn- parse-flags [args]
  (loop [args (lazy-seq args)
         result (hash-map)]
    (if (empty? args)
      result
      (let [curr (first args)
            rst (rest args)]
        (if (or (keyword? (first rst))
                (empty? rst))
          (recur rst (assoc result curr true))
          (recur (rest rst)
                 (assoc result curr (first rst))))))))

(defn- module-member-keys [module]
  (into [] (.getMemberKeys module)))

(defn module-member-by-key [module key]
  (.getMember module key))

(defmacro js. [obj method & args]
  `((if (.canInvokeMember ~obj ~(str method))
      (wrap-polyglot-invoke-member ~obj ~(str method))
      (wrap-polyglot-execute (.getMember ~obj ~(str method))))
    ~@args))

(defmacro js.- [obj field]
  `(clojurify-value (.getMember ~obj ~(str field))))

(defmacro js.. [obj & args]
  (if (empty? args)
    obj
    (let [curr# (first args)
          rest# (rest args)]
      `(js.. ~(cond (seq? curr#)
                    `(js. ~obj ~(first curr#) ~@(rest curr#))

                    (.startsWith (str curr#)
                                 "-")
                    `(js.- ~obj ~(subs (str curr#) 1))
                    :else
                    `(js. ~obj ~curr#))
         ~@rest#))))

(defn instantiate [obj & args]
  (.newInstance obj (into-array Object (map polyglotalize-clojure args))))

(defn create-js-array [& args]
  (apply instantiate js/Array args))

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


(defn clj->js [obj]
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
        (let [js-obj (instantiate js/Object)]
          (doseq [[k v] obj]
            (.putMember js-obj (clj->js k) (clj->js v)))
          js-obj)

        :else obj))

(defn js->clj [^org.graalvm.polyglot.Value value & {:keys [keywordize-keys] :or {keywordize-keys false}}]
  (let [f (fn thisfn [^org.graalvm.polyglot.Value value]
            #dbg!
            (cond (polyglot-primitive-type? value)
                  (polyglot-primitive-to-clj value)

                  (javascript-simple-function? value)
                  (wrap-polyglot-execute value)

                  (array? value)
                  (polyglot-iterable->vector value thisfn)

                  :else
                  (polyglot-object->map value
                                        :key-fn (if keywordize-keys
                                                  keyword
                                                  identity)
                                        :value-fn thisfn)))]
    (f value)))

(defn require-js [[module-name & flags]]
  (let [flag-map (parse-flags flags)
        module (require-module module-name)
        alias (:as flag-map)]
    (when alias
      (create-ns alias)
      (doseq [k (module-member-keys module)]
        (intern alias
                (symbol k)
                (clojurify-value (module-member-by-key module k)))))))


(defn- define-primitive [ns primitive]
  (intern ns (symbol primitive)
          (clojurify-value (.eval *context* "js" primitive))))

(defmacro define-primitives [ns & primitives]
  (create-ns ns)
  `(doseq [primitive# '~primitives]
     (define-primitive '~ns (str primitive#))))

(define-primitives js4clj.js
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
  Promise)
