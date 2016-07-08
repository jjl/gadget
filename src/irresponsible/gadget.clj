(ns irresponsible.gadget
  (:require [clojure.reflect :as r]
            [clojure.string :as str]
            [irresponsible.gadget.grep :as g]
            [irresponsible.gadget.inspector :as i]
            [irresponsible.gadget.summary :as s]
            [irresponsible.gadget.util :as u])
  (:refer-clojure :exclude [bases methods name type flatten]))

(doseq [n `[g/flatten  g/flatten-lens g/grep
            i/inspect i/name i/visibility i/public?
            i/protected? i/private? i/static? i/dynamic?
            i/bases i/constructors i/fields i/methods
            i/params i/returns i/type
            s/summary u/partial*]]
  (u/reexport *ns* n))

(defn gadget
  "Given a thing, creates a string describing it.
   Opts are passed to `summary`
   args: [thing] [thing opts]
   returns: string"
  ([thing]
   (gadget thing {:shorten? true}))
  ([thing opts]
   (-> thing inspect (summary opts))))

(def wtf?
  "Like gadget, but prints its output to the terminal
   args: [thing] [thing opts]
   returns: nil"
  (comp print gadget))
  
