(ns irresponsible.gadget.util
  (:require [clojure.core :as cc]
            [clojure.string :as str]
            [irresponsible.overload :refer [overload]])
  (:import  [java.lang StringBuilder]
            [clojure.reflect Constructor Method Field])
  (:refer-clojure :exclude [sorted-set-by]))

(defrecord PrimitiveInspector
  [type])

(defrecord ArrayInspector
  [type])

(defn load-class
  "Attempts to load the given named class
   args: [sym] fully qualified symbol naming a class
   returns: class object or nil"
  [sym]
  (when-let [[t c] (overload sym)]
    (when (= :class t)
      c)))

(defn load-param
  "Attempts to load the given *thing*, a symbol naming a class or type
   args: [sym] fully qualified symbol naming a class or type
   returns: class object, primitive or array inspector or nil"
  [sym]
  (letfn [(simple [[t v :as r]]
            (case t
              :class v
              :primitive (map->PrimitiveInspector {:type v})
              nil))]
    (when-let [[t v :as r] (overload sym)]
      (if (= t :array)
        (map->ArrayInspector {:type (simple v)})
        (simple r)))))

(defn uncurry
  "Takes a function and returns a function of one argument which applies
   the functions to the argument which should be a vector or seq
   args: [f]
   returns: function"
  [f]
  (fn [args]
    (apply f args)))

(defn assure
  "Returns a value but only when pred returns true
   args: [pred val]
   returns: val or nil"
  [pred val]
  (when (pred val)
    val))

(defn score-visibility
  "Returns a score for the visibility (keyword). Used for presentational ordering
   args: [visibility]
   returns: int"
  [vis]
  (case vis
    :public 1
    :protected 2
    :private 3
    4))

(defn cmp
  "[internal] For whatever stupid reason, sorted-set-by requires the
   function to be able to take a nil
   args: [f]
   returns: function"
  [f]
  (fn ([a] 0)
      ([a b] (f a b))
      ([a b & cs] (apply f a b cs))))

(defn sorted-group-by
  "Groups by the given group-fn into a sorted-map sorted by sort-fn
   args: [group-fn sort-fn coll]
   returns: sorted map"
  [group-fn sort-fn coll]
  (->> coll
       (group-by group-fn)
       (into (sorted-map-by (cmp sort-fn)))))

(defn sorted-set-by
  "Creates a set sorted by the given function, optionally adding entries
   args: [f] [f vals]
   returns: sorted set"
  ([f]
   (cc/sorted-set-by (cmp f)))
  ([f vals]
   (-> f sorted-set-by (into vals))))

(defn over-map
  "[internal] transduces over a map
   args: [t f map]
     t: transducer-returning function, e.g. map
     f: function to apply to the values
   returns: map"
  [t f m]
  (into (empty m) (t (uncurry f)) m))

(defn over-vals
  "[internal] helper for map-vals, filter-vals
   args: [t f map]
     t: transducer-returning function, e.g. map
     f: function to apply to the values
   returns: map"
  [t f m]
  (over-map t (fn [k v] [k (f v)]) m))

(defn map-vals
  "Returns a map modified by the application of a function to the values
   args: [f map]
   returns: map, possibly with different values"
  [f m]
  (over-vals map f m))

(defn filter-vals
  "Returns a map filtered by the application of a predicate to the values
   args: [pred map]
   returns: map, possibly with fewer entries"
  [p m]
  (over-map filter #(p %2) m))

(defn to-string
  "Turns something into a string. given a keyword, will not print the leading :
   args: [thing]
   returns: string"
  [k]
  (cond (string? k)  k
        (keyword? k) (cc/name k)
        :else        (str k)))

(defn str-concat
  "Given a nested vector of items, strips out nulls and flattens them,
   then concatenates them together
   args: [items]
   returns: string"
  [items]
  (apply str (filter (complement nil?) (flatten items))))

(defn classname
  "Returns the unqualified portion of a fully qualified symbol
   args: [sym]
   returns: string"
  [s]
  (->> (-> s str (.split "[./]") seq last)
       (assure (every-pred identity (partial not= "")))))

(defn make-name
  "Given a vector of namespace piece symbols, turns them into a namespace
   prefix for a class (ends with a dot.)
   args: [ps]
   returns: string
   ex: ['java 'lang] => \"java.lang.\""
  [ps]
  (str/join "." (conj (vec ps) "")))

(defn short-name
  "Returns a namespace qualified portion generated from the initial character
   of namespace components to reference a class
   args: [ps], a vector of namespace segment symbols
   returns: string
   ex: [foo bar] => \"foo.bar.\""
  [ps]
  (make-name (map #(.charAt ^String (str %) 0) ps)))

(defn -replace
  "Internal function that replaces a string only at the start of the string
   args: [str [haystack needle]]
   returns: string"
  [^String s [t r]]
  (let [t (str t) r (str r)]
    (if (.startsWith s t)
      (.replaceFirst s t r)
      s)))

(defn shorten
  "Shortens a symbol fully qualified name for output purposes by abbreviating
   known namespaces and using unqualified names for well-known types
   args: [sym]
   returns: symbol"
  [c]
  (let [cs '{java.lang.String              String
             java.lang.Byte                Byte
             java.lang.Character           Character
             clojure.lang.PersistentVector PersistentVector}
        rs  (->> '[[java lang] [java util] [java net] [java]
                   [clojure lang] [clojure core] [clojure tools] [clojure]]
                 (into [] (map #(vector (make-name %) (short-name %)))))]
    (or (cs c)
        (symbol (reduce -replace (str c) rs)))))

(defn maybe-shorten
  "If shorten?, shorten the symbol, else return it unmodified
   args: [sym shorten?]
   returns: symbol"
  [v shorten?]
  (if shorten?
    (shorten v)
    v))

(defn maybe-shorten-all
  "Like maybe-shorten, but over a vector
   args: [vs shorten?]
   returns: vs, possibly modified"
  [vs shorten?]
  (if shorten?
    (mapv shorten vs)
    vs))

(defn get-params
  "Returns the method parameter types vector as a string
   args: [inspector opts]
     opts: map of options. supported keys:
       :shorten? : boolean, whether the shorten the fully qualified names in output
   returns: string"
  [{:keys [params] :as t} opts]
  (let [{:keys [shorten?] :or [nil]} (or opts {})]
    (->> (maybe-shorten-all params shorten?)
         (into '[self])
         pr-str)))

(defn get-ctor-params
  "Returns the constructor parameter types vector as a string
   args: [inspector opts]
     opts: map of options. supported keys:
       :shorten? : boolean, whether the shorten the fully qualified names in output
   returns: string"
  [{:keys [params] :as t} opts]
  (let [{:keys [shorten?] :or [nil]} (or opts {})]
    (->> (maybe-shorten-all params shorten?)
         (into [])
         pr-str)))

(defn has-flag?
  "True if the thing has the given keyword flag
   args: [flag inspector]
   returns: boolean"
  [flag t]
  (-> t :flags (contains? flag)))

(defn visibility
  "Turns the visibility of the inspected thing into a keyword
   args: inspector
   returns: keyword or nil"
  [t]
  (cond (has-flag? :public    t) :public
        (has-flag? :protected t) :protected
        (has-flag? :private   t) :private
        :otherwise               nil))
