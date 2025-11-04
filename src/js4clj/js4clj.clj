(ns js4clj.js4clj
  (:import
   [java.util HashMap]
   [org.graalvm.polyglot Context Source Value]))

(defonce ^:dynamic *context* (-> (Context/newBuilder (into-array String ["js"]))
                                 (.allowExperimentalOptions true)
                                 (.options (HashMap.
                                            {"js.commonjs-require" "true"
                                             "js.commonjs-require-cwd" (str (System/getProperty "user.dir")
                                                                            "/node_modules")}))
                                 (.allowIO true)
                                 (.build)))

(defn- polyglotalize-clojure [value]
  (-> *context*
      (.asValue value)))

(declare clojurify-value)
(defn- wrap-polyglot-executable [^org.graalvm.polyglot.Value value]
  (fn [& args]
    (clojurify-value (.execute value (into-array Object (map polyglotalize-clojure args))))))

(defn wrap-polyglot-invoke-member [^org.graalvm.polyglot.Value obj ^String method]
  (fn [& args]
    (clojurify-value (.invokeMember obj method (into-array Object (map polyglotalize-clojure args))))))

(defn- clojurify-value [^org.graalvm.polyglot.Value value]
  (cond (.isBoolean value)
        (.asBoolean value)

        (.isNull value)
        nil

        (.isNumber value)
        (cond (.fitsInLong value)
              (.asLong value)

              (.fitsInDouble value)
              (.asDouble value))

        (.hasMembers value)
        value
        ;; (.canInstantiate value)
                ;; ;; TODO

        (.canExecute value)
        ;; TODO
        (wrap-polyglot-executable value)

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
  `((wrap-polyglot-invoke-member ~obj ~(str method))
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
