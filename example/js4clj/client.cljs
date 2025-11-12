(ns js4clj.client
  (:require [js4clj.react-wrapper :as r]
            ["react" :as react]
            ["react-dom/client" :as dom-client]
            [js4clj.components :as components]))


(defn init []
  (let [root (js/document.getElementById
              "root")
        home (react/createElement components/home)]
    (if js/window.__js4clj_ssr
      (dom-client/hydrateRoot root home)
      (.render (dom-client/createRoot root)
               home))))
