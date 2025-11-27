(ns net.coruscation.js4clj.api.utils
  (:import
   [java.nio.file Path]))

(defn str->path [s]
  (Path/of s (into-array String [])))

(defn normalize-path-to-string [path]
  (let [path (if (string? path)
               (Path/of path (into-array Object []))
               path)]
    (.toString (.normalize path))))
