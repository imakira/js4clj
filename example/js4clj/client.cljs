(ns js4clj.client
  (:require [js4clj.react-wrapper :as r]
            ["react" :as react]
            ["react-dom/client" :as dom-client]
            [js4clj.components :as components]))


(defn init []
  (-> (js/document.getElementById
       "root")
      (dom-client/createRoot)
      (.render (react/createElement components/home))))
