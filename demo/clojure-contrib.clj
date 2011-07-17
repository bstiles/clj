#!/usr/bin/env clj

(comment (defenv clj-env
           (:dependencies [[clojure-contrib "1.2.0"]])))

(use 'clojure.contrib.math)
(println (abs -42))
