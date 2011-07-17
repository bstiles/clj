#!/usr/bin/env clj

(comment (defenv clj-env))

(println "clojure-with-args" (apply str (interpose "|" *command-line-args*)))
