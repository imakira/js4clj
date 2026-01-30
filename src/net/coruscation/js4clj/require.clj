(ns net.coruscation.js4clj.require
  (:require
   [clojure.string :as string]
   [net.coruscation.js4clj.api.converting :refer :all]
   [net.coruscation.js4clj.api.modules :as modules]
   [net.coruscation.js4clj.api.polyglot :refer :all]))

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

(defn load-es-module
  "Load a module using `import()`, return the module object"
  [module-name]
  (modules/load-es-module module-name))


(defn load-commonjs-module
  "Load a module using `require`, return the module object."
  [module-name]
  (modules/load-commonjs-module module-name))


(defn require-helper
  [require-module-fn]
  (letfn [(do-require ([[module-name & flags]]
                       (when-not (string? module-name)
                         (throw (ex-info "module-name must be a string"
                                         {:module-name module-name
                                          :expected-type String
                                          :received-type (class module-name)})))
                       (let [flag-map (parse-flags flags)
                             module (require-module-fn module-name)
                             alias-name (:as flag-map)
                             qualified-module-name (symbol (str "js4clj.modules." (normalize-module-name module-name)))]
                         (when-not (symbol? alias-name)
                           (throw (ex-info "alias must be a string"
                                           {:alias alias-name
                                            :expected-type clojure.lang.Symbol
                                            :received-type (class alias-name)})))
                         (internal-module module qualified-module-name alias-name))))]
    (fn [modules]
      (doseq [module modules]
        (do-require module)))))

(defn require-js
  "(require-js '[\"module-name\" :alias ns])

  Import a JavaScript module and add its exports to namespace `ns`

  Like node.js, this function finds JavaScript modules from the node_modules directory,
    it supports ECMAScript modules and legacy CommonJS modules."
  {:clj-kondo/lint-as 'clojure.core/require}
  [& module-specs]
  ((require-helper load-es-module) module-specs))

(defn require-cjs
  "Like `require-js` but use `require` internally instead of `import`

  It exists mainly due to `import` provided by GraalJS will error out when there's an `exports` field in the package.json, even when `main` field is presented. While `require` will ignore `exports`.

  Also check https://github.com/oracle/graaljs/pull/904, this function will be useless if the upstream issue is resolved."
  {:clj-kondo/lint-as 'clojure.core/require}
  [& module-specs]
  ((require-helper load-commonjs-module) module-specs))
