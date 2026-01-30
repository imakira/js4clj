(ns net.coruscation.js4clj.utils)

(defmacro select-keys-bind [keys obj & body]
  `(let [{:keys [~@keys]}
         ~obj]
     ~@body))
