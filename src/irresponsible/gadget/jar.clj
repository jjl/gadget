(ns irresponsible.gadget.jar
  (:require [clojure.string :as s]
            [irresponsible.unscrew :as u]))

(defn normalise-class
  "Given the jar-relative path of a class, turns it into a java classname
   args: [path]
   returns: string"
  [path]
  (-> path
       (s/replace #"\.class$" "")
       (s/replace #"/" ".")))

(defn normalise-namespace
  "Given the jar-relative path of a clojure file, turns it into a namespace name
   args: [path]
   returns: string"
  [path]
  (-> path
       (s/replace #"\.clj[cs]?$" "")
       (s/replace #"/" ".")
       (s/replace #"_" "-")))

(defn namespaces-in-jar
  "Returns a sequence of file paths in the jar that look like clojure(script) files
   args: [jar]
   returns: seq of string"
  [jar]
  (u/entries-matching jar (partial re-find #"\.clj[sc]?$")))
  
(defn classes-in-jar
  "Returns a sequence of file paths in the jar that look like java classes
   args: [jar]
   returns: seq of string"
  [jar]
  (u/entries-matching jar (partial re-find #"\.class$")))
