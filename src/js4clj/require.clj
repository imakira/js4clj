(ns js4clj.require
  (:require
   [cheshire.core :refer [parse-string]]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [js4clj.context :refer [*context*]]
   [js4clj.core :refer :all]
   [js4clj.api.utils :refer [normalize-path-to-string]]
   [clojure.core.async :as a])
  (:import
   [java.nio.file Path]
   [java.util.regex Pattern]))

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

(defn- normalize-module-name [name]
  (string/replace (str name) #"/" "."))

(defn- internal-module [^org.graalvm.polyglot.Value module qualified-module-name & [alias-name]]
  (create-ns qualified-module-name)
  (doseq [k (.getMemberKeys module)]
    (intern qualified-module-name
            (symbol k)
            (clojurify-value (.getMember module k))))
  (when alias-name
    (alias alias-name qualified-module-name)))

(defn- require-module-dynamic [name]
  (let  [module-promise (-> *context*
                            (.eval "js"
                                   (str "import('" name "')")))
         chann (a/chan 1)]
    (invoke-member module-promise "then"
                   (wrap-clojure-fn
                    (fn [m]
                      (a/>!! chann m))))
    (a/<!! chann)))

(defn require-js
  {:clj-kondo/lint-as 'clojure.core/require}
  ([[module-name & flags]]
   (let [flag-map (parse-flags flags)
         module (require-module-dynamic module-name)
         alias-name (:as flag-map)
         qualified-module-name (symbol (str "js4clj.modules." (normalize-module-name module-name)))]
     (internal-module module qualified-module-name alias-name)))
  ([curr & colls]
   (require-js curr)
   (when colls (apply require-js colls))))

