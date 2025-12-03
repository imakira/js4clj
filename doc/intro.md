# Usage

# Prerequisite & Installation

TODO


<a id="org71358e7"></a>

# Usage


<a id="org0757d24"></a>

## Requiring Namespaces

```clojure
(ns net.coruscation.js4clj.example
  (:require
   [net.coruscation.js4clj.require :refer [require-js]]
   [net.coruscation.js4clj.utils :refer [js. js.- js.. clj->js js->clj js-set!]]
   [net.coruscation.js4clj.core :refer [js-new]]
   [net.coruscation.js4clj.js :as js]))
```


<a id="orgea97a50"></a>

## Using JavaScript Libraries

Install JavaScript libraries in the project root using npm like usual.

```bash
npm init
npm i luxon # luxon as an example
```

```clojure
;; we can use luxon as a namespace now, for example: luxon/DateTime
(require-js '["luxon" :as luxon])
```


<a id="orgf88f3b7"></a>

## Converting data between Clojure and JavaScript

This library provides two functions similar to those of ClojureScript: `js->clj` and `clj->js`.

```clojure
;; result: #object[org.graalvm.polyglot.Value 0x22b60c71 "{key: \"value\"}"]
(def js-obj (clj->js {:key "value"}))
;; result: {:key "value"}
(js->clj js-obj :keywordize-keys true) 
```


<a id="orgbbec24a"></a>

## Calling JavaScript functions, get/set JavaScript properties

Similar to ClojureScript, this libraries provides four macros: `js.` `js.-` `js..` and `js-set!`

We can use `js.` to call a method of an Object.

```clojure
(def current-time (js. luxon/DateTime now))
;; result: "2025-11-27T23:27:34.853+08:00"
(js. current-time toString) 
```

We can use `js.-` to access properties of an object.

```clojure
;; result: "value"
(js.- (clj->js {:key "value"})
      key) 
```

We can also use `js..` to chain the calling

```clojure
(js.. luxon/DateTime now toString)
```

We can also use `js..` to access property by using `-` as a prefix:

```clojure
(js.. luxon/DateTime
  (fromObject (clj->js {:day 22}))
  (plus (clj->js {:day 1}))
  ;; results: 23
  -day) 
```

The library also provides a `js-set!`, it must be used along with `js.-` or `js..`.

```clojure
(let [obj (clj->js {})]
  (js-set! (js.- obj key)
           "value")

  (js.- obj key) ;; "value"

  (js-set! (js.. obj -key)
           "new value")
  (js.- obj key) ;; "new value"
  )
```


<a id="orgd8c1da3"></a>

# Examples

You can check this [minimal Server Side Rendering with Client Side Rehydration example using react](https://github.com/imakira/js4clj/tree/master/example/js4clj).


<a id="orgdada725"></a>

# Links

This project is inspired by:

-   [libpython-clj](https://github.com/clj-python/libpython-clj)
-   [py4clj](https://github.com/plandes/clj-py4j)

Check this blog about roughly how it is done [Experiment with GraalVM, Clojure, and JavaScript.](https://coruscation.net/blogs/experiment-with-graalvm--clojure--and-javascript.html)
