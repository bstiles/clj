(ns net.bstiles.clj.main
  (:use [net.bstiles.clj.core :only [find-env run-in-env safe-read]])
  (:use [clojure.contrib.reflect :only [call-method]])
  (:require [clojure.java.io :as io]
            [clojure.main]))

(defn load-exec-fn
  [env-type & args]
  (let [env-ns-name (str "net.bstiles.clj.env." env-type)]
    (load (str "/" (.replace env-ns-name \. \/)))
    (require (symbol env-ns-name))
    (when-let [ns (find-ns (symbol env-ns-name))]
      (when-let [exec-fn (ns-resolve ns (symbol env-ns-name "make-exec-fn"))]
       (apply exec-fn args)))))

#_(defn get-additional-deps
  [env-type]
  (when-let [env-ns-name (str "net.bstiles.clj.env." env-type)]
    (load (str "/" (.replace env-ns-name \. \/)))
    (require (symbol env-ns-name))
    (when-let [ns (find-ns (symbol env-ns-name))]
      (ns-resolve ns (symbol env-ns-name "additional-deps")))))

(when-let [$1 (first *command-line-args*)]
  (when (.exists (io/file $1))
    (let [text (safe-read $1)]
      (if-let [env (find-env text)]
        (run-in-env env (apply load-exec-fn (:env-type env) *command-line-args*))
        (apply clojure.main/main *command-line-args*)))))




#_(when-let [$1 (first *command-line-args*)]
  (when (.exists (io/file $1))
    (let [text (safe-read $1)]
      (if-let [env (find-env text)]
        (run-in-env env (make-call-clojure-main-fn
                         #_(comment "-e" (str "(require 'swank.swank)"
                                              "(swank.swank/start-repl 4010)"))
                         $1))
        (apply clojure.main/main *command-line-args*)))))

