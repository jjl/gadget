(ns irresponsible.gadget.jar
  (:require [classlojure.core :as c]
            [clojure.string :as s]
            [irresponsible.unscrew :as u]))

(defn normalise-class [path]
  (->> path
       (s/replace #"\.class$" "")
       (s/replace #"/" ".")))

(defn normalize-clojure [path]
  (->> path
       (s/replace #"\.clj$" "")
       (s/replace #"/" ".")))

(defn clojure-in-jar [jar]
  (u/get-entries-matching jar (partial re-find #"\.clj[sc]?$")))
  
(defn classes-in-jar [jar]
  (u/get-entries-matching jar (partial re-find #"\.class$")))
