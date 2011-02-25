(ns net.bstiles.clj.env.rhino
  (:use [clojure.contrib.reflect :only [call-method]]))

(defn make-exec-fn
  [& args]
  (fn [class-loader]
    (let [rhino-main (.. class-loader (loadClass "org.mozilla.javascript.tools.shell.Main"))
          args (into-array String args)]
      (call-method rhino-main :main [(class args)] nil args))))