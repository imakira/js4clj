(ns net.coruscation.js4clj.embedding-test
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is]]
   [clojure.tools.build.api :as b]
   [net.coruscation.js4clj.embedding :as subject])
  (:gen-class))

(def class-dir "target/class")

(def basis (b/create-basis {:project "deps.edn"}))

(deftest test-uber
  (b/delete {:path "target"})
  (b/copy-dir {:src-dirs ["src" "resources" "embedding_test"]
               :target-dir class-dir})

  (subject/embed class-dir)

  (b/compile-clj {:basis basis
                  :src-dirs ["src" "embedding_test"]
                  :class-dir class-dir})

  (b/uber {:class-dir class-dir
           :uber-file "target/test-embedding.jar"
           :basis basis
           :main 'net.coruscation.js4clj.test-embedding
           :conflict-handlers {#".*" :warn}})

  (let [tmp-dir (.toString (java.nio.file.Files/createTempDirectory "js4clj-test" (into-array java.nio.file.attribute.FileAttribute [])))
        source-jar  "target/test-embedding.jar"
        current-time (java.time.ZonedDateTime/now)]
    (io/copy (io/file source-jar)
             (io/file (str tmp-dir
                           "/"
                           "testing.jar")))
    (shell/with-sh-dir tmp-dir
      (let [shell-result (shell/sh "java" "-jar" "testing.jar")]
        (is (< (.getSeconds (java.time.Duration/between current-time
                                                        (java.time.ZonedDateTime/parse
                                                         (.trim (:out shell-result)))))
               60))))))
