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
                "(comment clj-env (:dependencies [[a/b \"1.0\"]]))"))))
  (testing "Multiple dependencies."
    (is (= '[[a/b "1.0"]
            [b/c "2.0"]]
             (find-dependencies
              "(comment clj-env (:dependencies [[a/b \"1.0\"] [b/c \"2.0\"]]))"))))
  (testing "Spans lines."
    (is (= '[[a/b "1.0"]]
             (find-dependencies
              (str "(comment" \newline
                   "clj-env" \newline
                   "(:dependencies" \newline
                   "[[a/b \"1.0\"]]))")))))
  (testing "Not starting on first line."
    (is (= '[[a/b "1.0"]]
             (find-dependencies
              (str "#!/usr/bin/env clj"\newline
                   "(comment clj-env (:dependencies [[a/b \"1.0\"]]))")))))
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
                   "(comment" \newline
                   "clj-env" \newline
                   "(:dependencies" \newline
                   "[[a/b \"1.0\"]]))")))))
  (testing "First one wins."
    (is (= '[[a/b "1.0"]]
             (find-dependencies
              (str "(comment" \newline
                   "clj-env" \newline
                   "(:dependencies" \newline
                   "[[a/b \"1.0\"]]))" \newline
                   "(comment" \newline
                   "clj-env" \newline
                   "(:dependencies" \newline
                   "[[x/x \"1.0\"]]))"))))))

(deftest not-finding-dependencies
 (testing "No tag."
   (is (nil? (find-dependencies
              "(comment (:dependencies [[a/b \"1.0\"]]))"))))
 (testing "Not in comment."
   (is (nil? (find-dependencies
              "(clj-env (:dependencies [[a/b \"1.0\"]]))"))))
 (testing "No key."
   (is (nil? (find-dependencies
              "(comment clj-env ([[a/b \"1.0\"]]))"))))
 (testing "Commented out."
   (is (nil? (find-dependencies
              ";; (comment clj-env (:dependencies [[a/b \"1.0\"]]))")))))

(deftest runaway
  (testing "Declaration is within the permitted distance from the beginning."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (apply str (concat (interpose " " (range 99))
                               '("(comment clj-env (:dependencies [[a/b \"1.0\"]]))")))))))
  (testing "Declaration past the permitted distance from the beginning."
    (is (nil? (find-dependencies
               (apply str (concat (interpose " " (range 100))
                                  '("(comment clj-env (:dependencies [[a/b \"1.0\"]]))"))))))))

(deftest multiple-objects-on-the-same-line
  (testing "Dependencies first."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "(comment clj-env (:dependencies [[a/b \"1.0\"]]))"
                 "0")))))
  (testing "Dependencies last."
    (is (= '[[a/b "1.0"]]
           (find-dependencies
            (str "0"
                 "(comment clj-env (:dependencies [[a/b \"1.0\"]]))"))))))

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