(ns irresponsible.gadget.grep
  (:import [java.util.regex Pattern]
           [clojure.lang AFunction LazySeq PersistentList PersistentList$EmptyList
            PersistentArrayMap PersistentHashMap PersistentVector])
  (:refer-clojure :exclude [flatten]))

(defprotocol Grep
  (-grep [p s]))

(defprotocol Walky
  (flatten [self])
  (-flatten-lens [self lens]))

(defn flatten-lens [v]
  (partition 2 (-flatten-lens v [])))

(defn grep
  "Calls `flatten-lens` and searches over the data
   args: [pat subj]
     pat: pattern to match. string/regex/fn
     subj: nested data to search
   returns: seq of (val lens) lists
     val:  leaf data
     lens: the path taken to get there
   note: the lens is searched as well."
  [pattern subj]
  (->> (flatten-lens subj)
       (filter (fn [[v l]] (some (partial -grep pattern) (cons v l))))
       doall))

(extend-protocol Grep
  String
  (-grep [p s]
    (when (not= -1 (.indexOf (str s) p))
      [s]))
  Pattern
  (-grep [p s]
    (when (re-find p (str s))
      [s]))
  AFunction
  (-grep [p s]
    (when (p s)
      [s])))

(defn flatten-map [m]
  (doall (mapcat (fn [[k v]] (cons k (flatten v))) m)))

(defn flatten-lens-map [m l]
  (doall (mapcat (fn [[k v]] (-flatten-lens v (conj l k))) m)))
  
(defn flatten-lens-seq [s l]
  (-> #(-flatten-lens %2 (conj l %))
      (mapcat (range) s)
      doall))

(extend-protocol Walky
  LazySeq
  (flatten       [s]   (mapcat flatten s))
  (-flatten-lens [s l] (flatten-lens-seq s l))
  PersistentList$EmptyList
  (flatten       [s]   [])
  (-flatten-lens [s l] [nil l])
  PersistentList
  (flatten       [s]   (mapcat flatten s))
  (-flatten-lens [s l] (flatten-lens-seq s l))
  PersistentVector
  (flatten       [s]   (mapcat flatten s))
  (-flatten-lens [s l] (flatten-lens-seq s l))
  PersistentHashMap
  (flatten       [m]   (flatten-map m))
  (-flatten-lens [m l] (flatten-lens-map m l))
  PersistentArrayMap
  (flatten       [m]   (flatten-map m))
  (-flatten-lens [m l] (flatten-lens-map m l))
  Object
  (flatten       [v]   [v])
  (-flatten-lens [v l] [v l])
  nil
  (flatten       [_]   [nil])
  (-flatten-lens [_ l] [nil l]))
