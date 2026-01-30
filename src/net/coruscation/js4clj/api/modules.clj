(ns net.coruscation.js4clj.api.modules
  (:require
   [clojure.core.async :as a]
   [net.coruscation.js4clj.api.polyglot :refer :all]
   [net.coruscation.js4clj.api.utils :refer [str->path]]
   [net.coruscation.js4clj.context :refer [*context* *context-per-thread*]]))

(defn load-es-module
  "Load a module using `import()`, return the module object"
  [module-name]
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
                       (reify org.graalvm.polyglot.proxy.ProxyExecutable
                         (execute [_ ^"[Lorg.graalvm.polyglot.Value;" values]
                           (a/>!! chann (first values)))))
        (invoke-member "catch"
                       (reify org.graalvm.polyglot.proxy.ProxyExecutable
                         (execute [_ ^"[Lorg.graalvm.polyglot.Value;" values]
                           (a/>!! error (first values))))))

    (a/alt!! chann ([module] module)
             error ([e] (throw (ex-info "Module Import Error"
                                        {:type ::module-import-error
                                         :error e}))))))

(defn load-commonjs-module
  "Load a module using `require`, return the module object."
  [name]
  (let  [module (-> @*context*
                    (.eval "js"
                           (str "require('" name "')")))]
    (assert (.hasMembers module)
            (str name " is not a module"))
    module))

