(ns net.bstiles.clj.core
  (:import (java.io PushbackReader StringReader))
  (:require [clojure.java.io :as io]))

(def
  ^{:doc "Maximum number of objects read before giving up finding an env form."}
  *runaway-count* 100)

(defn find-env
  "Search Clojure-readable text for a specially marked form containing an
  environment definition.  The form must be contained in a comment macro
  with a first argument of `clj-env'."
  [text]
  (let [eof-value (Object.)
        r (PushbackReader. (io/reader (StringReader. text)))
        candidate (loop [object (read r nil eof-value)
                         runaway-counter 1]
                    (if (< *runaway-count* runaway-counter)
                      nil
                      (if (and (seq? object)
                               (= ['comment 'clj-env] [(first object) (second object)]))
                        (try
                          (apply hash-map (nth object 2))
                          (catch Exception e nil))
                        (recur (read r nil eof-value)
                               (inc runaway-counter)))))]
    (when-not (= eof-value candidate)
      candidate)))

(comment
  (defn make-managed-class-environment
    [& args]
    (let [{:keys [app-path parent-path aspect-path weave-path client-path]} (apply hash-map args)
          parent-loader (loop [paths (filter seq [app-path (concat parent-path aspect-path)])
                               loader (.getParent (ClassLoader/getSystemClassLoader))]
                          (if-let [path (first paths)]
                            (recur (next paths)
                                   (URLClassLoader. (into-array URL (map io/as-url path)) loader))
                            loader))
          weaving-loader (when weave-path
                           (proxy [WeavingURLClassLoader] [(into-array URL (map io/as-url weave-path))
                                                           parent-loader]
                             (getAspectURLs [] (into-array URL (map io/as-url aspect-path)))))]
      (cond
       (seq client-path) (URLClassLoader. (into-array URL (map io/as-url client-path))
                                          (or weaving-loader parent-loader))
       weaving-loader weaving-loader
       :else parent-loader)))

  (defn standard-scripted-environment
    [script-path orig-path & [aspect-path third-party-filter-fn]]
    (let [clojure-path (for [jar-name *clojure-jars*]
                         (if-let [jar-url (get-file-resource jar-name)]
                           jar-url
                           (fail-jar-not-found jar-name)))]
      (if aspect-path
        (let [third-party (filter (or third-party-filter-fn *third-party-filter-fn*) orig-path)
              ours (filter (comp not (apply hash-set third-party)) orig-path)]
          (make-managed-class-environment
           :app-path (for [jar-name *aspectj-jars*]
                       (if-let [jar-url (get-file-resource jar-name)]
                         jar-url
                         (fail-jar-not-found jar-name)))
           :parent-path third-party
           :aspect-path aspect-path
           :weave-path ours
           :client-path (concat script-path clojure-path)))
        (make-managed-class-environment :parent-path orig-path
                                        :client-path (concat script-path clojure-path)))))

  (defn run-clojure-in
    [env & args]
    (.setContextClassLoader (Thread/currentThread) env)
    (let [rt (.. env (loadClass "clojure.lang.RT"))
          clojure-main (.. env (loadClass "clojure.main"))
          args (into-array String args)]
      (call-method clojure-main :main [(class args)] nil args))))
