(ns net.bstiles.clj.main
  (:use net.bstiles.clj.core)
  (:use [clojure.contrib.reflect :only [call-method]])
  (:require [clojure.java.io :as io]
            [clojure.main]))

(load-file "/Users/bstiles/.clojure.d/swank-init/user.clj")


(when-let [$1 (first *command-line-args*)]
  (when (.exists (io/file $1))
    (let [text (safe-read $1)]
      (if-let [env (user/dbg (find-env text))]
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

