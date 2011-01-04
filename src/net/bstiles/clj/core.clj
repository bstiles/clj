(ns net.bstiles.clj.core
  (:import (java.io PushbackReader StringReader)
           (java.net URL URLClassLoader))
  (:use [clojure.contrib.reflect :only [call-method]])
  (:require [clojure.java.io :as io]
            [mcp-deps.aether :as deps]))

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

(defn- reformat-artifact-ids
  [ids]
  (for [[id version] ids]
    (let [group-id (or (namespace id) (name id))
          artifact-id (name id)
          version version]
      [group-id artifact-id version])))

(defn make-class-loader-env
  "Creates a chain of class loaders according to spec.  By default,
  clojure.jar and clojure-contrib.jar will be added to the child-most
  class loader and will be removed from any parent class loaders.  If
  an AspectJ weaving class loader is specified, the AspectJ jars will
  be added to the constructed class loader and removed from any parent
  class loaders.

  Simplest classpath
  ==================

  [[group-id/artifact-id \"version\"] ...]

  Creates a single class loader parented by the parent of
  ClassLoader/getSystemClassLoader.  This class loader will have as its
  classpath all the dependencies specified by the Maven artifact
  descriptors.

  Chain of loaders
  ================

  (chain
    [[group-id/artifact-id \"version\"] ...]                  <= 0..n
    [:aspectj [group-id/artifact-id \"version\"] ...]         <= 0..1
    [[group-id/artifact-id \"version\"] ...])                 <= 0..n

  Creates a chain of one or more class loaders, one for each spec
  in the chain-list.  The first spec is parented by the parent of
  ClassLoader/getSystemClassLoader.  Each specified class loader is
  made the parent of the next specified class loader in the chain-list.

  A specification with a first element of :aspectj will construct
  a weaving class loader that will weave aspects contained in the
  specified graph of artifacts."
  [env]
  (let [deps (:dependencies env)]
    (cond
     (= 'chain (first deps)) (throw (UnsupportedOperationException. "Chains not implemented."))
     (every? sequential? deps) (URLClassLoader.
                                (into-array
                                 URL
                                 (for [dep (:files (deps/resolve-runtime-artifacts
                                                    (reformat-artifact-ids deps)))]
                                   (-> dep .toURI .toURL)))
                                (.getParent (ClassLoader/getSystemClassLoader)))
     :else (throw (RuntimeException. (format "Unrecognized environment specification: %s"
                                             (pr-str env)))))))

(defn run-in-env
  "Creates a class loader environment from the specification, creates
  a thread, sets its context class loader, and runs run-fn in the
  thread, passing the leaf class loader as the only argument.  See
  make-class-loader-env for the format of env-spec."
  [env-spec run-fn]
  (let [class-loader (make-class-loader-env env-spec)]
   (doto (Thread. (ThreadGroup. "clj")
                  #(run-fn class-loader))
     (.setContextClassLoader class-loader)
     .start)))

(defn make-call-clojure-main-fn
  [& args]
  (fn [class-loader]
    (let [clojure-main (.. class-loader (loadClass "clojure.main"))
          args (into-array String args)]
      (call-method clojure-main :main [(class args)] nil args))))

