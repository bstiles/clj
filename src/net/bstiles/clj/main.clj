(ns net.bstiles.clj.main
  (:use net.bstiles.clj.core)
  (:require [clojure.java.io :as io]
            [clojure.main]))

(when-let [$1 (first *command-line-args*)]
  (when (.exists (io/file $1))
    (let [text (slurp $1)]
      (if-let [env (find-env text)]
        (run-in-env env (make-call-clojure-main-fn $1))
        (apply clojure.main/main *command-line-args*)))))

