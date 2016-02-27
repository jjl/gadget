(ns irresponsible.gadget.util
  (:require [clojure.core :as cc]
            [clojure.string :as str])
  (:import  [java.lang StringBuilder]
            [clojure.reflect Constructor Method Field])
  (:refer-clojure :exclude [sorted-set-by]))

(defn uncurry [f]
  (partial apply f))

(defn assure [pred val]
  (when (pred val)
    val))

(defn score-visibility
  [t]
  (case t
    :public 1
    :protected 2
    :private 3
    4))

(defn sorted-group-by [f c]
  (->> c (group-by f) (into (sorted-map))))

(defn sorted-set-by [f]
  (cc/sorted-set-by (fn [& vs]
                      (apply compare (map f vs)))))

(defn over-vals [t f m]
  (into (empty m) (t (uncurry f)) m))

(defn map-vals [f m]
  (over-vals (map (uncurry #(vector % (f %2))) m)))

(defn filter-vals [p m]
  (over-vals filter #(p %2) m))

(defn looks-slashey? [^String s]
  (not= -1 (.indexOf s "/")))

(def primitives #{'byte 'char 'short 'int 'long 'float 'double 'boolean})

(declare load-symbol)

(defn try-array [s]
  (let [^String s (str s)
        i (.lastIndexOf s "<>")]
    (when (= i (- (.length s) 2))
      [:array (-> s (.substring 0 i) symbol load-symbol)])))

(defn try-primitive [s]
  (when (primitives s)
    [:primitive s]))

(defn try-symbol [s]
  (try
    [:symbol (resolve s)]
    (catch Exception e nil)))

(defn load-symbol [s]
  (or (try-array s)
      (try-primitive s)
      (try-symbol s)))

(defn to-string
  [k]
  (cond (string? k)  k
        (keyword? k) (cc/name k)
        :else        (str k)))

(defn str-concat [items]
  (apply str (filter (complement nil?) (flatten items))))

(defn classname [c]
  (-> c (.split "\\.") seq last))

(defn make-name [ps]
  (str/join "." (conj (vec ps) "")))

(defn short-name [ps]
  (make-name (map #(.charAt ^String (str %) 0) ps)))

(defn alias-name [n]
  (->> n str (re-find #"[^\./]+$")))

(defn -replace [^String acc [t r]]
  (let [t (str t) r (str r)]
    (if (.startsWith acc t)
      (.replaceFirst acc t r)
      acc)))

(defn shorten [c]
  (let [cs '{java.lang.String              String
             java.lang.Byte                Byte
             java.lang.Character           Character
             clojure.lang.PersistentVector PersistentVector}
        rs  (->> '[[java lang] [java util] [java net] [java]
                   [clojure lang] [clojure core] [clojure tools]]
                 (into [] (map #(vector (make-name %) (short-name %)))))]
    (or (cs c)
        (symbol (reduce -replace (str c) rs)))))

(defn maybe-shorten [v shorten?]
  (if shorten?
    (shorten v)
    v))

(defn maybe-shorten-all [vs shorten?]
  (if shorten?
    (mapv shorten vs)
    vs))

(defn get-params [{:keys [params] :as t} opts]
  (let [{:keys [shorten?] :or [nil]} (or opts {})]
    (->> (maybe-shorten-all params shorten?)
         (into '[self])
         pr-str)))

(defn get-ctor-params [{:keys [params] :as t} opts]
  (let [{:keys [shorten?] :or [nil]} (or opts {})]
    (->> (maybe-shorten-all params shorten?)
         (into [])
         pr-str)))

(defn has-flag?
  "True if the thing has the given flag"
  [flag t]
  (-> t :flags (contains? flag)))

(defn visibility
  "Turns the visibility of the thing into a keyword"
  [t]
  (cond (has-flag? :public    t) :public
        (has-flag? :protected t) :protected
        (has-flag? :private   t) :private
        :otherwise               nil))

