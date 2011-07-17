(ns net.bstiles.clj.core
  (:import (java.io PushbackReader StringReader)
           (java.net URL URLClassLoader)
           (java.nio CharBuffer)
           (java.util.regex Pattern))
  (:use [clojure.contrib.reflect :only [call-method]])
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [mcp-deps.aether :as deps]
            [net.bstiles.clj.util :as util]))

(def
  ^{:doc "Maximum number of objects read before giving up finding an env form."}
  *runaway-count* 100)

(def
  ^{:doc "Additional dependencies to be added to the environment by default."}
  *additional-deps* '[[org.clojure/clojure "1.2.0"]
                         [org.clojure/clojure-contrib "1.2.0"]
                         [swank-clojure "1.3.0-SNAPSHOT"]])

(defn safe-read
  "Reads at most the first 8,000 characters of a file."
  [file]
  (with-open [reader (io/reader file)]
    (let [cb (CharBuffer/allocate (* 8 1000))]
      (loop [c (.read reader cb)]
        (if (or (= -1 c)
                (= 0 (.length cb)))
          (.. cb flip toString)
          (recur (.read reader cb)))))))

(defn strip-comment-patterns
  "Searches the text for one or two regex patterns defining a single line
  or multi-line start/end comment pattern.  The text is then returned with
  these comment delimiters removed.  The one or two regex patterns will also
  be removed.

  The regex patterns must reside in a single-line environment definition
  header.

  Examples
  ========

  The following defines a single line comment delimiter:

      ... defenv #\"//\" ???-env

  ??? may be any sequence of word characters.

  The following defines a multi-line comment delimiter pair:

      .. defenv #\"<!--\" #\"-->\" ???-env"
  [text]
  (let [[_ start end] (re-find #"defenv #\"(.+?)(?<!\\)\"(?: #\"(.+?)(?<!\\)\")? \w+-env" text)]
    (loop [patterns [start end]
           text (string/replace text
                                #"(defenv) #\".+?(?<!\\)\"(?: #\".+?(?<!\\)\")? (\w+-env)"
                                "$1 $2")]
      (if-let [pattern (first patterns)]
        (recur (next patterns)
               (string/replace text (Pattern/compile pattern) ""))
        text))))

(defn- match-definition
  [object]
  (when (seq? object)
    (if (and (= 'defenv (first object))
             (symbol? (second object))
             (re-matches #"\w+-env" (str (second object))))
      object
      (when (= 'comment (first object))
        (seq? (second object))
        (loop [xs (next object)]
          (when-let [x (first xs)]
            (if (and (= 'defenv (first x))
                     (symbol? (second x))
                     (re-matches #"\w+-env" (str (second x))))
              x
              (recur (next xs)))))))))

(defn find-env
  "Search Clojure-readable text for a specially marked form containing an
  environment definition.  The form must be have a first element of
  `defenv' and a second element of `<type>-env' (ignoring the one or two
  optional regexes specifying comment delimiters to strip)."
  [text]
  (let [eof-value (Object.)
        r (PushbackReader. (io/reader (StringReader. text)))
        read-fn #(try (read r nil eof-value)
                      (catch Exception e nil))
        candidate (loop [object (read-fn)
                         runaway-counter 1]
                    (if (< *runaway-count* runaway-counter)
                      nil
                      (if-let [form (match-definition object)]
                        (try
                          (assoc (apply hash-map (last form))
                            :env-type (string/replace (name (second form))
                                                      #"-env$"
                                                      ""))
                          (catch Exception e nil))
                        (recur (read-fn)
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
  "Creates a chain of class loaders according to spec.  If an AspectJ
  weaving class loader is specified, the AspectJ jars will be added to
  the constructed class loader and removed from any parent class
  loaders.

  Simplest classpath
  ==================

  [[group-id/artifact-id \"version\"] ...]

  Creates a single class loader parented by the parent of
  ClassLoader/getSystemClassLoader.  This class loader will have as
  its classpath all the dependencies specified by the Maven artifact
  descriptors.

  Chain of loaders
  ================

  (chain
    [[group-id/artifact-id \"version\"] ...]                  <= 0..n
    [:aspectj [group-id/artifact-id \"version\"] ...]         <= 0..1
    [[group-id/artifact-id \"version\"] ...])                 <= 0..n

  Creates a chain of one or more class loaders, one for each spec in
  the chain-list.  The first spec is parented by the parent of
  ClassLoader/getSystemClassLoader.  Each specified class loader is
  made the parent of the next specified class loader in the
  chain-list.

  A specification with a first element of :aspectj will construct
  a weaving class loader that will weave aspects contained in the
  specified graph of artifacts."
  [env & opts]
  (let [deps (:dependencies env)
        opts (apply hash-map opts)
        offline (or (:offline opts)
                    (= "true" (System/getProperty "net.bstiles.clj.offline")))]
    (cond
     (= 'chain (first deps)) (throw (UnsupportedOperationException. "Chains not implemented."))
     (every? sequential? deps) (URLClassLoader.
                                (into-array
                                 URL
                                 (for [dep (:files (deps/resolve-runtime-artifacts
                                                    (reformat-artifact-ids
                                                     (concat
                                                      deps
                                                      (:additional-deps opts)))
                                                    :offline offline))]
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
  (let [class-loader (make-class-loader-env env-spec :additional-deps *additional-deps*)]
    (when (System/getProperty "net.bstiles.clj.debug-classpath")
      (util/print-classpath class-loader))
    (let [orig-context-class-loader (.getContextClassLoader (Thread/currentThread))]
      (try
        (.setContextClassLoader (Thread/currentThread)
                                class-loader)
        (run-fn class-loader)
        (finally
         (.setContextClassLoader (Thread/currentThread)
                                 orig-context-class-loader))))))

(defn make-call-clojure-main-fn
  "Returns a function taking a single java.lang.ClassLoader argument and
  loads clojure.main from that class loader and calls its main method.
  Reflection is used so that the clojure.main class is loaded from the
  supplied class loader rather than the class loader environment of the
  function returned by this function.

  The arguments supplied to this function will be passed to the
  clojure.main/main method."
  [& args]
  (fn [class-loader]
    (let [clojure-main (.. class-loader (loadClass "clojure.main"))
          args (into-array String args)]
      (call-method clojure-main :main [(class args)] nil args))))

