(ns js4clj.server
  (:require [hiccup2.core :as h]
            [hiccup.util :refer [raw-string]]
            [net.coruscation.js4clj.require :refer [require-js]]
            [js4clj.components :as comps]
            [js4clj.react-wrapper :as wrapper]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.file :refer [wrap-file]]))

(require-js '["react-dom/client" :as dom-client])
(require-js '["react" :as react])
;; I know it doesn't look like the brightest idea
(require-js '["./node_modules/react-dom/cjs/react-dom-server-legacy.browser.development.js" :as dom-server])

(defn template [content]
  (str
   "<!DOCTYPE html>"
   (h/html
       [:html.no-js {:lang "en"}
        [:head
         [:meta {:charset "utf-8"}]
         [:meta {:http-equiv "x-ua-compatible" :content "ie=edge"}]
         [:meta {:name "description" :content ""}]
         [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
         [:title "Title"]
         [:link {:rel "preconnect", :href "https://fonts.googleapis.com"}]
         #_[:link {:rel "stylesheet" :href (str "/main.css?v=" (System/currentTimeMillis))}]]
        [:body
         [:div#root (raw-string content)]
         [:script (raw-string "window.__js4clj_ssr = true")]
         [:script {:src "/js/main.js"}]]])))

(defn home [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (template (dom-server/renderToString (wrapper/r comps/home)))})

(def handler (-> #'home
                 (wrap-file "./public")))

(defonce ^:dynamic *jetty* (jetty/run-jetty #'handler {:port 3001 :join? false}))
