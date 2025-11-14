(ns js4clj.require
  (:require
   [clojure.java.io :as io]
   [clojure.string :as s]
   [js4clj.context :refer [*context*]]
   [js4clj.core :refer :all]))

(defn- require-commjs-module [name]
  (let  [module (-> *context*
                    (.eval "js"
                           (str "require('" name "')")))]
    (assert (.hasMembers module)
            (str name " is not a module"))
    module))

(defn- require-esm-module [path]
  (let [url (io/resource (str path))
        source (.build (org.graalvm.polyglot.Source/newBuilder "js" url))]
    (.eval *context* source)))

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
  (s/replace (str name) #"/" "."))

(defn- internal-module [^org.graalvm.polyglot.Value module qualified-module-name & [alias-name]]
  (create-ns qualified-module-name)
  (doseq [k (.getMemberKeys module)]
    (intern qualified-module-name
            (symbol k)
            (clojurify-value (.getMember module k))))
  (when alias-name
    (alias alias-name qualified-module-name)))

(defn require-js
  {:clj-kondo/lint-as 'clojure.core/require}
  ([[module-name & flags]]
   (let [flag-map (parse-flags flags)
         module (require-commjs-module module-name)
         alias-name (:as flag-map)
         qualified-module-name (symbol (str "js4clj.modules." (normalize-module-name module-name)))]
     (internal-module module qualified-module-name alias-name)))
  ([curr & colls]
   (require-js curr)
   (when colls (apply require-js colls))))

(defn require-esm
  {:clj-kondo/lint-as 'clojure.core/require}
  ([[module-name & flags]]
   (let [flag-map (parse-flags flags)
         module (require-esm-module module-name)
         alias-name (:as flag-map)
         qualified-module-name (symbol (str "js4clj.esmodules." (normalize-module-name module-name)))]
     (internal-module module qualified-module-name alias-name)))
  ([curr & colls]
   (require-esm curr)
   (when colls (apply require-esm colls))))
