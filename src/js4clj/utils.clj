(ns js4clj.utils
  (:require [js4clj.core :refer :all]
            [js4clj.js :as js]))

#_{:clj-kondo/ignore [:syntax]}
(defmacro js.
  ;; TODO
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [obj method & args]
  `((if (.canInvokeMember ~obj ~(str method))
      (wrap-polyglot-invoke-member ~obj ~(str method))
      (wrap-polyglot-execute (.getMember ~obj ~(str method))))
    ~@args))

(defmacro js.-
  {:clj-kondo/ignore [:unresolved-symbol :type-mismatch]}
  [obj field]
  `(#'clojurify-value (.getMember ~obj ~(str field))))

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
                  (polyglot-primitive->-clj value)

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
