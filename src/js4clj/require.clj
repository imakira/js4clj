(ns js4clj.require
  (:require
   [cheshire.core :refer [parse-string]]
   [clojure.java.io :as io]
   [clojure.string :as s]
   [clojure.string :as string]
   [js4clj.context :refer [*context*]]
   [js4clj.core :refer :all]
   [js4clj.api.utils :refer [normalize-path-to-string]])
  (:import
   [java.nio.file Path]
   [java.util.regex Pattern]))

(defn- require-commjs-module [name]
  (let  [module (-> *context*
                    (.eval "js"
                           (str "require('" name "')")))]
    (assert (.hasMembers module)
            (str name " is not a module"))
    module))

(defn- require-esm-module [path]
  (let [url (.toURL (io/file path))
        source (-> (org.graalvm.polyglot.Source/newBuilder "js" url)
                   (.mimeType "application/javascript+module")
                   (.build))]
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

(defn require-cjs
  {:clj-kondo/lint-as 'clojure.core/require}
  ([[module-name & flags]]
   (let [flag-map (parse-flags flags)
         module (require-commjs-module module-name)
         alias-name (:as flag-map)
         qualified-module-name (symbol (str "js4clj.modules." (normalize-module-name module-name)))]
     (internal-module module qualified-module-name alias-name)))
  ([curr & colls]
   (require-cjs curr)
   (when colls (apply require-cjs colls))))

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

(defn- find-package-json [path]
  (loop [path (Path/of (.toURI (io/file path)))]
    (if (nil? path)
      nil
      (let [f (.toFile (.resolve path "package.json"))]
        (if (.exists f)
          {:directory (normalize-path-to-string (.toAbsolutePath path))
           :content (parse-string (slurp f))}
          (recur (.getParent path)))))))

(defn- build-pattern-from-wildcard [s]
  (->> (string/split s #"\*")
       (map (fn [x] (Pattern/quote x)))
       (string/join "([^\\./]*)")
       (Pattern/compile)))

(defn- select-result-in-exports [candidates]
  (cond (string? candidates)
        candidates

        :else
        (or (get candidates "default")
            (get candidates "browser")
            (get candidates "import")
            (get candidates "require")
            (get candidates "node"))))

(defn- wildcard-apply-matches [s matches]
  (loop [s s
         matches matches]
    (if (empty? matches)
      s
      (recur (string/replace-first s #"\*" (first matches))
             (rest matches)))))

(defn- find-module-with-exports [path-to-match exports]
  (let [exports (if (string? exports) {"." exports} exports)
        path-to-match (.normalize path-to-match)
        to-match-str (if (= (.toString path-to-match) "")
                       "." (str "./" (.toString path-to-match)))]
    (->> exports
         (map (fn [[match value]]
                (let [matches (re-matches (build-pattern-from-wildcard match) to-match-str)]
                  (if matches
                    (if (string? matches)
                      (select-result-in-exports value)
                      (wildcard-apply-matches (select-result-in-exports value)
                                              (rest matches)))
                    nil))))
         (filter (complement nil?))
         first)))

(defn- load-module [path module-type]
  (cond (.endsWith path ".cjs")
        (require-commjs-module path)

        (.endsWith path ".mjs")
        (require-esm-module path)

        (= module-type :esm)
        (require-esm-module path)

        (= module-type :cjs)
        (require-commjs-module path)

        :else
        (require-esm-module path)))

(defonce js-extensions ["mjs" "js" "cjs"])

(defn- find-file-with-possible-extensions [path-str extensions]
  (some->> extensions
           (filter (fn [ext]
                     (.exists (io/file (str path-str "." ext)))))
           (first)
           (str path-str ".")))

(defn- find-module-simple [module]
  ;; if the path ends with an extension
  (let [module-path (.toPath (io/file module))]
    (cond (some (fn [ext]
                  (.endsWith (.toString module-path)
                             ext))
                js-extensions)
          module-path

          ;; if the path is a folder
          (and (.exists (.toFile module-path))
               (.isDirectory (.toFile module-path)))
          (find-file-with-possible-extensions (.toString (.resolve module-path "index"))
                                              js-extensions))))



(defn- find-module-with-package-json [module package-json package-json-directory]
  (let [module-path (.toPath (.getAbsoluteFile (io/file module)))
        package-json-path (.toPath (io/file package-json-directory))
        path-to-match (.relativize package-json-path module-path)
        exports (get package-json "exports")]
    (cond
      ;; find by `exports` field
      exports
      (some->> (find-module-with-exports path-to-match exports)
               (.resolve package-json-path)
               normalize-path-to-string)

      ;; find by `main` field
      (and (= (normalize-path-to-string path-to-match)
              "")
           (get package-json "main"))
      (->> (get package-json "main")
           (.resolve package-json-path)
           normalize-path-to-string)

      :else nil)))

(defn- find-module [module-name]
  (let [module-name (if (.startsWith module-name "./")
                      module-name
                      (str "node_modules/" module-name))
        {package-json :content
         package-json-directory :directory} (find-package-json module-name)
        module-location (or (and package-json
                                 (find-module-with-package-json
                                  module-name
                                  package-json
                                  package-json-directory))
                            (find-module-simple module-name))
        module-type (if package-json
                      (if (= (get package-json "type")
                             "module")
                        :esm
                        :cjs)
                      :default)]
    (when module-location
      {:module-location module-location
       :module-type module-type})))

(defn require-js  {:clj-kondo/lint-as 'clojure.core/require}
  ([[module-name & flags]]
   (let [flag-map (parse-flags flags)
         alias-name (:as flag-map)
         qualified-module-name (symbol (str "js4clj.esmodules." (normalize-module-name module-name)))
         {:keys [module-location module-type]} (find-module module-name)]

     (when (nil? module-location)
       (throw (ex-info "Module can't be found" {:module-name module-name})))
     (internal-module
      (load-module module-location module-type)
      qualified-module-name alias-name)))
  ([curr & colls]
   (require-js curr)
   (when colls (apply require-js colls))))
