(ns net.bstiles.clj.test.core
  (:import (java.net URLClassLoader))
  (:use [net.bstiles.clj.core] :reload)
  (:use [clojure.test]))

(defn find-dependencies
  [text]
  (:dependencies (find-env text)))

(deftest finding-dependencies
  (testing "Trivial case."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            "(defenv clj-env (:dependencies [[a/b \"1.0\"]]))"))))
  (testing "Multiple dependencies."
    (is (= '[[a/b "1.0"]
             [b/c "2.0"]]
           (find-dependencies
            "(defenv clj-env (:dependencies [[a/b \"1.0\"] [b/c \"2.0\"]]))"))))
  (testing "Spans lines."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "(defenv" \newline
                 "clj-env" \newline
                 "(:dependencies" \newline
                 "[[a/b \"1.0\"]]))")))))
  (testing "Not starting on first line."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "#!/usr/bin/env clj"\newline
                 "(defenv clj-env (:dependencies [[a/b \"1.0\"]]))")))))
  (testing "At the end of the file."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "; Now is the time for all good men" \newline
                 "; to come to the aid of their country." \newline
                 "; Now is the time for all good men" \newline
                 "; to come to the aid of their country." \newline
                 "; Now is the time for all good men" \newline
                 "; to come to the aid of their country." \newline
                 "; Now is the time for all good men" \newline
                 "; to come to the aid of their country." \newline
                 "; Now is the time for all good men" \newline
                 "; to come to the aid of their country." \newline
                 "; Now is the time for all good men" \newline
                 "; to come to the aid of their country." \newline
                 "; Now is the time for all good men" \newline
                 "; to come to the aid of their country." \newline
                 "(println 1)"
                 "(defenv" \newline
                 "clj-env" \newline
                 "(:dependencies" \newline
                 "[[a/b \"1.0\"]]))")))))
  (testing "First one wins."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "(defenv" \newline
                 "clj-env" \newline
                 "(:dependencies" \newline
                 "[[a/b \"1.0\"]]))" \newline
                 "(defenv" \newline
                 "clj-env" \newline
                 "(:dependencies" \newline
                 "[[x/x \"1.0\"]]))")))))
  (testing "Embedded in a Clojure comment form."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "(comment (defenv clj-env (:dependencies [[a/b \"1.0\"]])))")))))
  (testing "Embedded in a C-style multi-line comment."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "/*(defenv clj-env (:dependencies [[a/b \"1.0\"]]))*/")))))
  (testing "Embedded in a C-style multi-line comment spanning lines."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "/*" \newline
                 "(defenv clj-env (:dependencies [[a/b \"1.0\"]]))" \newline
                 "*/")))))
  (testing "Embedded in a C-style single-line comment spanning lines."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "/*" \newline
                 "(defenv clj-env (:dependencies [[a/b \"1.0\"]]))" \newline
                 "*/"))))))

(deftest find-options
  (testing ":offline"
    (is (:offline (find-env "(comment (defenv clj-env (:offline true)))")))
    (is (:offline (find-env "(comment (defenv clj-env (:dependencies [[a/b \"1.0\"]] :offline true)))")))
    (is (not (:offline (find-env "(comment (defenv clj-env")))))
  (testing ":include-sources"
    (is (:include-sources (find-env "(comment (defenv clj-env (:include-sources true)))")))
    (is (not (:include-sources (find-env "(comment (defenv clj-env))"))))))

(deftest not-finding-dependencies
 (testing "No tag."
   (is (nil? (find-dependencies
              "(defenv (:dependencies [[a/b \"1.0\"]]))"))))
 (testing "Not in comment."
   (is (nil? (find-dependencies
              "(clj-env (:dependencies [[a/b \"1.0\"]]))"))))
 (testing "No key."
   (is (nil? (find-dependencies
              "(defenv clj-env ([[a/b \"1.0\"]]))"))))
 (testing "Commented out."
   (is (nil? (find-dependencies
              ";; (defenv clj-env (:dependencies [[a/b \"1.0\"]]))")))))

(deftest runaway
  (testing "Declaration is within the permitted distance from the beginning."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (apply str (concat (interpose " " (range 99))
                               '("(defenv clj-env (:dependencies [[a/b \"1.0\"]]))")))))))
  (testing "Declaration past the permitted distance from the beginning."
    (is (nil? (find-dependencies
               (apply str (concat (interpose " " (range 100))
                                  '("(defenv clj-env (:dependencies [[a/b \"1.0\"]]))"))))))))

(deftest large-file
  (testing "Declaration is within first 8kb."
    (let [f (doto (java.io.File/createTempFile "clj" "tmp")
              .deleteOnExit)]
      (spit f (apply str (concat '("(defenv clj-env (:dependencies [[a/b \"1.0\"]]))")
                                 (repeat (* 10 1000) "a"))))
      (is (= '[[a/b "1.0"]]
             (find-dependencies (safe-read f))))))
  (testing "Declaration is outside first 8kb."
    (let [f (doto (java.io.File/createTempFile "clj" "tmp")
              .deleteOnExit)]
      (spit f (apply str (concat (repeat (* 10 1000) "a")
                                 '("(defenv clj-env (:dependencies [[a/b \"1.0\"]]))"))))
      (is (nil? (find-dependencies (safe-read f)))))))

(deftest multiple-objects-on-the-same-line
  (testing "Dependencies first."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "(defenv clj-env (:dependencies [[a/b \"1.0\"]]))"
                 "0")))))
  (testing "Dependencies last."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "0"
                 "(defenv clj-env (:dependencies [[a/b \"1.0\"]]))"))))))

(deftest make-environments
  (testing "Simplest case group/artifact."
    (let [cl-env (make-class-loader-env
                  {:dependencies '[[org.clojure/clojure "1.2.0"]]})]
      (is (= URLClassLoader (class cl-env)))
      (is (some #(re-find #"\bclojure.*[.]jar" (str %))
                (.getURLs cl-env)))))
  (testing "Simplest case abbreviated artifact."
    (let [cl-env (make-class-loader-env
                  {:dependencies '[[ant "1.7.0"]]})]
      (is (= URLClassLoader (class cl-env)))
      (is (some #(re-find #"\bant.*[.]jar" (str %))
                (.getURLs cl-env))))))

(deftest comment-stripping
  (testing "C-style single line."
    (is (= " foo\ndefenv clj-env\n bar"
           (strip-comment-patterns (str "// foo" \newline
                                        "//defenv #\"//\" clj-env" \newline
                                        "// bar")))))
  (testing "C-style multi-line."
    (is (= " foo\ndefenv clj-env\nbar"
           (strip-comment-patterns (str "/* foo" \newline
                                        "defenv #\"/[*]\" #\"[*]/\" clj-env" \newline
                                        "bar*/")))))
  (testing "Hash single-line."
    (is (= " foo\n defenv clj-env\n bar"
           (strip-comment-patterns (str "# foo" \newline
                                        "# defenv #\"#\" clj-env" \newline
                                        "# bar")))))
  (testing "Semi-colon single-line."
    (is (= " foo\n defenv clj-env\n bar"
           (strip-comment-patterns (str "; foo" \newline
                                        "; defenv #\";\" clj-env" \newline
                                        "; bar")))))
  (testing "XML multi-line."
    (is (= " foo\n defenv clj-env\n bar "
           (strip-comment-patterns (str "<!-- foo" \newline
                                        " defenv #\"<!--\" #\"-->\" clj-env" \newline
                                        " bar -->")))))
  (testing "Fictional double-quote single-line."
    (is (= " foo\n defenv clj-env\n bar"
           (strip-comment-patterns (str "\" foo" \newline
                                        "\" defenv #\"\\\"\" clj-env" \newline
                                        "\" bar"))))))