#!/usr/bin/env clj

(comment (defenv clj-env
           (:dependencies [[swank-clojure "1.3.0-SNAPSHOT"]])))

(use 'swank.util.sys)
(println (user-home-path))
