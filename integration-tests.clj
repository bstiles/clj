(require '[clojure.java.io :as io])
(require '[clojure.contrib.io :as cio])
(use 'clojure.test)
(use 'mcp-core.sh)

(.setUncaughtExceptionHandler (Thread/currentThread)
                              (reify Thread$UncaughtExceptionHandler
                                (uncaughtException [_ thread throwable]
                                                   (.printStackTrace throwable)
                                                   ($exit))))

(when (= "NO_SOURCE_FILE" *file*)
  (throw (RuntimeException. "Can't run tests without knowing where we are!")))

(def *here* (.getParent (io/file *file*)))

(deftest demo-with-dependencies
  (is (= "<html></html>"
         ($> env ~(format "LAUNCHER_CLASSPATH=%s"
                          (System/getProperty "java.class.path"))
             ~(io/file *here* "scripts" "clj")
             "hello-world-with-deps.clj"))))

(run-tests)
;; Prevents the thread pools from causing the VM to linger
($exit)

