(ns net.bstiles.clj.core
  (:import (java.io PushbackReader StringReader))
  (:require [clojure.java.io :as io]))

(defn find-dependencies
  [text]
  (let [eof-value (Object.)
        r (PushbackReader. (io/reader (StringReader. text)))
        candidate (loop [object (read r nil eof-value)
                         runaway-counter 1]
                    (if (< 100 runaway-counter)
                      nil
                      (if (and (seq? object)
                               (= ['comment 'clj-env] [(first object) (second object)]))
                        (try
                          (:dependencies (apply hash-map (nth object 2)))
                          (catch Exception e nil))
                        (recur (read r nil eof-value)
                               (inc runaway-counter)))))]
    (when-not (= eof-value candidate)
      candidate)))
