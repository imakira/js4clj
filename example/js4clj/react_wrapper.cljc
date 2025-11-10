(ns js4clj.react-wrapper
  #?(:clj (:require [js4clj.require :refer [require-js]]
                    [js4clj.utils :refer [clj->js js->clj]])
     :cljs (:require ["react" :as react]))
  #?(:cljs (:require-macros [js4clj.react-wrapper :refer [defcomp if-cljs]])))

#?(:clj (require-js '["react" :as react]))

(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

#?(:clj (defmacro if-cljs
          "Return then if we are generating cljs code and else for Clojure code.

  https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
          [then else]
          (if (cljs-env? &env) then else)))

(defmacro defcomp
  [comp-name args & body]
  `(defn ~comp-name [args#]
     (let [~(first args) ((if-cljs
                            cljs.core/js->clj
                            js->clj)
                          args# :keywordize-keys true)]
       ~@body)))

(defn r [comp & [attrs & children]]
  #dbg!
  (apply react/createElement comp
         (clj->js attrs)
         children))

(defn use-state [init]
  (js->clj (react/useState init)))
