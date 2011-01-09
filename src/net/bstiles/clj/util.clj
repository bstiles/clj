(ns net.bstiles.clj.util
  (:require clojure.pprint))

(defn- get-classpath [^Class c]
  (loop [loader (.getClassLoader c) lines []]
    (if (nil? loader)
      lines
      (recur (.getParent loader)
             (concat [(str "-- " loader)]
                     (sort (map (memfn getPath) 
                                (seq (.getURLs loader))))
                     lines)))))

(defn print-classpath
  ([] (print-classpath (class (fn []))))
  ([^Class c] (binding [*print-level* nil *print-length* nil]
                (clojure.pprint/pprint (get-classpath c)))))
