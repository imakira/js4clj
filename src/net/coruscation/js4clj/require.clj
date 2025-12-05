(ns net.coruscation.js4clj.require
  (:require
   [clojure.core.async :as a]
   [clojure.string :as string]
   [net.coruscation.js4clj.api.converting :refer :all]
   [net.coruscation.js4clj.api.polyglot :refer :all]
   [net.coruscation.js4clj.api.utils :refer [str->path]]
   [net.coruscation.js4clj.context :refer [*context*]]))

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

(defn- require-module-dynamic [module-name]
  (let  [name (if (.startsWith module-name ".")
                ;; Some hacky things going on here:
                ;; `import` isn't really supposed to be abled to import
                ;;   CommonJS modules, here it can, for some reason.
                ;; It also isn't supposed to be able to import files that
                ;;   are not in the `exports` fields in package.json.
                ;; But the limitation doesn't seem to be fully implemented.
                ;; You can stil import one using aboslute path.
                ;; I consider it useful for some case.
				(.toString (.toAbsolutePath (str->path module-name)))
                module-name)
         module-promise (-> @*context*
                            (.eval "js"
                                   (str "import('" name "')")))
         chann (a/chan 1)
         error (a/chan 1)]
    (-> module-promise
        (invoke-member "then"
                       (wrap-clojure-fn
                        (fn [m]
                          (a/>!! chann m))))
        (invoke-member "catch"
                       (wrap-clojure-fn
                        (fn [e]
                          (a/>!! error e)))))

    (a/alt!! chann ([module] module)
             error ([e] (throw (ex-info "Module Import Error"
                                        {:type ::module-import-error
                                         :error e}))))))

(defn require-js
  "(require-js '[\"module-name\" :alias ns])

  Import a JavaScript module and add its exports to namespace `ns`

  Like node.js, this function finds JavaScript modules from the node_modules directory,
    it supports ECMAScript modules and legacy CommonJS modules."
  {:clj-kondo/lint-as 'clojure.core/require}
  ([[module-name & flags]]
   (when-not (string? module-name)
     (throw (ex-info "module-name must be a string"
                     {:module-name module-name
                      :expected-type String
                      :received-type (class module-name)})))
   (let [flag-map (parse-flags flags)
         module (require-module-dynamic module-name)
         alias-name (:as flag-map)
         qualified-module-name (symbol (str "js4clj.modules." (normalize-module-name module-name)))]
     (when-not (symbol? alias-name)
       (throw (ex-info "alias must be a string"
                       {:alias alias-name
                        :expected-type clojure.lang.Symbol
                        :received-type (class alias-name)})))
     (internal-module module qualified-module-name alias-name)))
  ([module & args]
   (require-js module)
   (when args (apply require-js args))))
