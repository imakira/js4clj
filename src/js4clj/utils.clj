(ns js4clj.utils
  (:require [js4clj.core :refer :all]
            [js4clj.js :as js :refer [builtin]]))

#_{:clj-kondo/ignore [:syntax]}
(defmacro js.
  ;; TODO
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [obj method & args]
  `(clojurify-value
    (apply invoke-member ~obj ~(str method)
           ;; evaluate args before passing  to polyglotalize-clojure
           (map polyglotalize-clojure (list ~@args)))))

(defmacro js.-
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [obj field]
  `(#'clojurify-value (get-member ~obj ~(str field))))

#_{:clj-kondo/ignore [:syntax]}
(defmacro js..
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [obj & args]
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

(defn create-js-array [& args]
  (apply instantiate (builtin "Array") args))

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
        (let [js-obj (instantiate (builtin "Object"))]
          (doseq [[k v] obj]
            (.putMember js-obj (clj->js k) (clj->js v)))
          js-obj)

        :else
        ;; extract js4clj.core/raw-value from obj if exists
        ;; wrap fn if (fn? obj)
        ;; https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html#asValue(java.lang.Object)
        ;; for polyglot's builtin types
        (polyglotalize-clojure obj)))

(defn js->clj [value & {:keys [keywordize-keys] :or {keywordize-keys false}}]
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
