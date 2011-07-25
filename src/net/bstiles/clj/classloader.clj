(ns net.bstiles.clj.classloader
  (:import (java.io IOException)
           (java.net URLClassLoader)
           (java.util.jar JarFile))
  (:use [clojure.contrib.reflect :only [get-field call-method]]))

(defn chained-access
  [obj & fields]
  (reduce #(get-field (class %1) %2 %1) obj fields))

(defn finalize-native-libs
  [class-loader]
  (try
    (when-let [native-libraries (chained-access class-loader "nativeLibraries")]
      (doseq [native-library (seq native-libraries)]
        (call-method (class native-library) "finalize" [] class-loader)))
    (catch Exception e)))

(defn close
  [class-loader]
  (try
    (when-let [loaders (chained-access class-loader "ucp" "loaders")]
      (let [jars (for [loader loaders]
                   (let [jar (chained-access loader "jar")]
                     (try
                       (.close jar)
                       (catch IOException e
                         (println e)))
                     jar))]
        (finalize-native-libs class-loader)
        (when-let [jar-url-connection-class (Class/forName "sun.net.www.protocol.jar.JarURLConnection")]
          (let [file-cache (try
                             (get-field jar-url-connection-class "fileCache" nil)
                             (catch NoSuchFieldException e
                               nil))
                url-cache (try
                            (get-field jar-url-connection-class "urlCache" nil)
                            (catch NoSuchFieldException e
                              nil))]
            (when file-cache
              (doseq [key (iterator-seq (.. file-cache clone keySet))]
                (let [jar-file (.get file-cache key)]
                  (when (and (instance? JarFile jar-file)
                             (contains? jars (.getName jar-file)))
                    (.close jar-file)
                    (.remove file-cache key)))))
            (when url-cache
              (doseq [jar-file (iterator-seq (.. url-cache clone keySet))]
                (when (and (instance? JarFile jar-file)
                           (contains? jars (.getName jar-file)))
                  (.close jar-file)
                  (when file-cache
                    (.remove file-cache (.get url-cache jar-file)))
                  (.remove url-cache jar-file))))))))
    (catch Exception e
      (println e))))

