(ns js4clj.require
  (:require [js4clj.context :refer [*context*]]
            [js4clj.core :refer :all]))

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

(defn require-js
  [[module-name & flags] & coll]
  (let [flag-map (parse-flags flags)
        module (require-module module-name)
        alias-name (:as flag-map)
        qualified-module-name (symbol (str "js4clj.modules." module-name))]
    (create-ns qualified-module-name)
    (doseq [k (.getMemberKeys module)]
      (intern qualified-module-name
              (symbol k)
              (clojurify-value (.getMember module k))))
    (when alias-name
      (alias alias-name qualified-module-name)))
  (when (seq coll)
    (apply require-js coll)))
