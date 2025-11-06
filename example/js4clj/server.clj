(ns js4clj.server
  (:require [js4clj.require :refer [require-js]]))

(require-js '["react-dom/client" :as client])
(require-js '["react" :as react])
(require-js '["react-dom/server.browser" :as server])

