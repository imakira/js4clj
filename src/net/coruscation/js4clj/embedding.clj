(ns net.coruscation.js4clj.embedding
  (:require [net.coruscation.js4clj.context :as context]
            [clojure.java.io :as io]))

(defn- copy-dir [src dst]
  (.mkdirs (io/file dst))
  (doseq [file (file-seq (io/file src))]
    (let [subpath (.relativize
                   (.toPath (io/file src))
                   (.toPath file))]
      (if (.isDirectory file)
        (.mkdirs (io/file (str dst "/" subpath)))
        (spit (io/file (str dst
                            "/"
                            subpath))
              (slurp file))))))

(defn embed
  "Call it during building the uberjar file.
  It bundles *js-cwd*/node_modules into the jar file, generates a manifest identified by *js-resource-path*.
  If the manifest file exists in classpath, js4clj will extract the node_modules bundled in the uberjar file, and use the extracted path as *js-cwd*.
  Useful when you want to bundled all resources into an uberjar."
  [class-dir]
  (copy-dir (str context/*js-cwd* "/node_modules")
            (str class-dir "/"
                 context/*js-resource-path*
                 "/node_modules"))
  (let [cwd-path (.toPath (io/file context/*js-cwd*))
        files (file-seq (io/file (str context/*js-cwd*
                                      "/node_modules")))
        manifest (into [] (for [file files]
                            (let [path-str (.toString (.relativize cwd-path
                                                                   (.toPath file)))]
                              (if (.isDirectory file)
                                (str path-str "/")
                                path-str))))]
    (spit (io/file (str class-dir
                        "/"
                        context/*js-resource-path*
                        "/"
                        "manifest.edn"))
          (pr-str manifest))))
