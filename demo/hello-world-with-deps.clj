#!/usr/bin/env clj

(comment (defenv clj-env
           (:dependencies [[hiccup "0.3.0"]])))

(use 'hiccup.core)
(println (html [:span "Hello, world!"]))
