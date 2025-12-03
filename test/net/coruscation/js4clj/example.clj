(ns net.coruscation.js4clj.example
  (:require
   [net.coruscation.js4clj.require :refer [require-js]]
   [net.coruscation.js4clj.utils :refer [js. js.- js.. clj->js js->clj js-set!]]
   [net.coruscation.js4clj.core :refer [js-new]]
   [net.coruscation.js4clj.js :as js]))


(require-js '["luxon" :as luxon])
(def js-obj (clj->js {:key "value"}))
(js->clj js-obj :keywordize-keys true)

;; Calling ~now~ function of luxon/DateTime

(js. luxon/DateTime now)

(def current-time (js. luxon/DateTime now))
(js. current-time toString)

(js.. luxon/DateTime now toString)

(js.. luxon/DateTime
  (fromObject (clj->js {:day 22}))
  (plus (clj->js {:day 1}))
  -day)

(js.- (clj->js {:key "value"})
      key) ;; "value"

(let [obj (clj->js {})]
  (js-set! (js.- obj key)
           "value")

  (js.- obj key) ;; "value"

  (js-set! (js.. obj -key)
           "new value")
  (js.- obj key) ;; "new value"
  )
