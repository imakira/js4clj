(ns js4clj.server
  (:require [js4clj.require :refer [require-js]]
            [js4clj.components :refer [home]]))

(require-js '["react-dom/client" :as dom-client])
(require-js '["react" :as react])
;; I know it doesn't look like the brightest idea
(require-js '["react-dom/cjs/react-dom-server-legacy.browser.development" :as dom-server])

(dom-server/renderToString (react/createElement home))
