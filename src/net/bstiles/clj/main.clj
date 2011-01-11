(ns net.bstiles.clj.main
  (:use net.bstiles.clj.core)
  (:use [clojure.contrib.reflect :only [call-method]])
  (:require [clojure.java.io :as io]
            [clojure.main]))
#_(load-file "/Users/bstiles/.clojure.d/swank-init/user.clj")



(defn load-exec-fn
  [env-type & args]
  (when-let [ns-name (str "net.bstiles.clj.env." env-type)]
    (load (str "/" (.replace ns-name \. \/)))
    (require (symbol ns-name))
    (when-let [ns (find-ns (symbol ns-name))]
      (when-let [exec-fn (ns-resolve ns (symbol ns-name "make-exec-fn"))]
       (apply exec-fn args)))))

(when-let [$1 (first *command-line-args*)]
  (when (.exists (io/file $1))
    (let [text (safe-read $1)]
      (if-let [env (find-env text)]
        (run-in-env env (load-exec-fn (:env-type env) $1))
        (apply clojure.main/main *command-line-args*)))))




#_(when-let [$1 (first *command-line-args*)]
  (when (.exists (io/file $1))
    (let [text (safe-read $1)]
      (if-let [env (find-env text)]
        (run-in-env env (fn [class-loader]
                          (let [clojure-main (.. class-loader (loadClass "org.jruby.Main"))
                                args (into-array String [$1])]
                            (call-method clojure-main :main [(class args)] nil args))))
        (apply clojure.main/main *command-line-args*)))))
#_(when-let [$1 (first *command-line-args*)]
  (when (.exists (io/file $1))
    (let [text (safe-read $1)]
      (if-let [env (find-env text)]
        (run-in-env env (fn [class-loader]
                          (let [clojure-main (.. class-loader (loadClass "org.mozilla.javascript.tools.shell.Main"))
                                args (into-array String [$1])]
                            (call-method clojure-main :main [(class args)] nil args))))
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

