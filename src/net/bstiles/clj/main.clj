(ns net.bstiles.clj.main
  (:use net.bstiles.clj.core)
  (:require [clojure.java.io :as io]
            [clojure.main]))

(defn run-in-env
  [text]
  (println "running with scissors"))

(when-let [$1 (first *command-line-args*)]
  (when (.exists (io/file $1))
    (let [text (slurp $1)]
      (if-let [deps (find-dependencies text)]
        (run-in-env text)
        (apply clojure.main/main *command-line-args*)))))

