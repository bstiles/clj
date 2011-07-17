(ns net.bstiles.clj.env.clj
  (:use [net.bstiles.clj.core :only [make-call-clojure-main-fn]]))

(defn make-exec-fn
  [& args]
  (apply make-call-clojure-main-fn
         #_(comment "-e" (str "(require 'swank.swank)"
                              "(swank.swank/start-repl 4010)"))
         args))
