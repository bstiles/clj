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

(deftest demo-without-dependencies
  (testing "No environment."
    (is (= "Hello, world!\n"
           ($> env ~(format "LAUNCHER_CLASSPATH=%s"
                            (System/getProperty "java.class.path"))
               ~(io/file *here* "scripts" "clj")
               ~(io/file *here* "demo" "hello-world.clj"))))))

(deftest demo-with-dependencies
  (testing "Arbitrary third-party library."
    (is (= "<span>Hello, world!</span>\n"
           ($> env ~(format "LAUNCHER_CLASSPATH=%s"
                            (System/getProperty "java.class.path"))
               ~(io/file *here* "scripts" "clj")
               ~(io/file *here* "demo" "hello-world-with-deps.clj")))))
  (testing "Clojure contrib."
    (is (= "42\n"
           ($> env ~(format "LAUNCHER_CLASSPATH=%s"
                            (System/getProperty "java.class.path"))
               ~(io/file *here* "scripts" "clj")
               ~(io/file *here* "demo" "clojure-contrib.clj")))))
  (testing "Swank-clojure."
    (is (= (format "%s\n" (System/getProperty "user.home"))
           ($> env ~(format "LAUNCHER_CLASSPATH=%s"
                            (System/getProperty "java.class.path"))
               ~(io/file *here* "scripts" "clj")
               ~(io/file *here* "demo" "swank-clojure.clj")))))
  (testing "Rhino."
    (is (= "Hello, Javascript world!\n"
           ($> env ~(format "LAUNCHER_CLASSPATH=%s"
                            (System/getProperty "java.class.path"))
               ~(io/file *here* "scripts" "clj")
               ~(io/file *here* "demo" "javascript-hello-world.clj"))))))

(run-tests)
;; Prevents the thread pools from causing the VM to linger
($exit)

