(ns net.coruscation.js4clj.embedding-test
  (:require
   [clojure.test :refer [deftest is]]
   [clojure.tools.build.api :as b]
   [net.coruscation.js4clj.embedding :as subject])
  (:gen-class))

(defn -main [& _])

(def class-dir "target/class")

(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" "0.1" "0.1"))

(defn test-uber [& _]
  (b/delete {:path "target"})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/copy-file {:src "test/net/coruscation/js4clj/embedding_test.clj"
                :target (str class-dir
                             "/"
                             "net/coruscation/js4clj/embedding_test.clj")})
  
  (b/compile-clj {:basis basis
                  :src-dirs ["src" "test"]
                  :class-dir class-dir})

  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'net.coruscation.js4clj.embedding-test
           :conflict-handlers {#".*" :warn}}))


#_(deftest embed-test
    (is (= true
           (subject/foo))))
