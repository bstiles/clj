#!/usr/bin/env clj

(comment clj-env
         (:dependencies [[hiccup "0.3.0"]]))

(use 'hiccup.core)
(println (html "Hello, world!"))
