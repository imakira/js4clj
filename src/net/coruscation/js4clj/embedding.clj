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
  "Called during building the jar file.
  It bundles *js-cwd*/node_modules into the jar file, generates a manifest identified by *js-resource-path*.
  So that js4clj can know whether to use node_modules bundled in the jar file by checking whether the manifest file exists."
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
