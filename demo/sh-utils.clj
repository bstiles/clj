#!/usr/bin/env clj
(comment (defenv clj-env
           (:dependencies [[mcp-core "1.0.0-SNAPSHOT"]])))

(ns user
  (:use mcp-core.sh)
  (:require [clojure.string :as string]))

($ echo ~(first *command-line-args*))
($exit)