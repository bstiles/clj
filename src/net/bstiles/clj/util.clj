(ns net.bstiles.clj.util
  (:require clojure.pprint))

(defn- get-classpath [^ClassLoader cl]
  (loop [loader cl lines []]
    (if (nil? loader)
      lines
      (recur (.getParent loader)
             (concat [(str "-- " loader)]
                     (sort (map (memfn getPath) 
                                (seq (.getURLs loader))))
                     lines)))))

(defn print-classpath
  ([^ClassLoader cl] (binding [*print-level* nil *print-length* nil]
                       (clojure.pprint/pprint (get-classpath cl)))))
