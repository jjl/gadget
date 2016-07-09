(ns irresponsible.gadget.inspector
  (:require [clojure.reflect :as r]
            [irresponsible.gadget.util :as u])
  (:import [clojure.reflect Constructor Method Field])
  (:refer-clojure :exclude [name bases methods type]))

;; do do do do do do do 

(defprotocol Inspectable
  (inspect [insp]
    "Inspect the given thing, returning some sort of Inspector
     args: [thing]"))

(defrecord ConstructorInspector
  [name visibility params])

(defrecord MethodInspector
  [name visibility static? params returns flags])

(defrecord FieldInspector
  [name visibility type static? flags])

(defrecord ClassInspector
  [bases constructors methods fields name static? flags])

(def name
  "Gets the name of the inspected thing
   args: [inspector]
   returns: string or nil (not an inspector)"
  :name)

(def visibility
  "one of: :public, :private, :protected, nil (not an inspector)
   args: [inspector]
   returns: keyword"
  :visibility)

(def public?
  "true if public
   args: [inspector]
   returns: boolean"
  (comp (partial = :public)
        visibility))

(def protected?
  "true if protected
   args: [inspector]
   returns: boolean"
  (comp (partial = :protected)
        visibility))

(def private?
  "true if private
   args: [inspector]
   returns: boolean"
  (comp (partial = :private)
        visibility))

(def static?
  "Returns true if static
   args: [inspector]
   returns: boolean"
  :static?)

(def dynamic?
  "Returns true if not static
   args: [inspector]
   returns: boolean"
  (complement static?))

(defn bases
  "Returns a vector of ClassInspector for each base of the class
   Basic types are returned as symbols
   args: [inspector]
   returns: [ClassInspector]"
  [insp]
  (mapv (comp inspect u/load-class) (:bases insp)))

(defn constructors
  "Returns the constructors of the class
   args: [insp] [insp filter-fn]
   returns: map of name to inspector"
  ([insp]
   (:constructors insp))
  ([insp filter-fn]
   (->> insp :constructors (filter filter-fn))))

(defn fields
  "Returns the fields/properties of the class
   args: [insp] [insp filter-fn]
   returns: map of name to inspector"
  ([insp]
   (:fields insp))
  ([insp filter-fn]
   (->> insp :fields (u/filter-vals filter-fn))))

(defn methods
 "Returns the methods of the class
  args: [insp] [insp filter-fn]
  returns: map of name to inspector"
  ([insp]
   (:methods insp))
  ([insp filter-fn]
   (->> insp :methods (u/filter-vals filter-fn))))
 
(defn params
  "Get the vector of param types (as symbols) the invocable is invoked with
   args: [inspector]
   returns: [symbol]"
  [insp]
  (->> insp :params (mapv u/load-param)))

(def returns
  "Get the return type of the invocable
   args: [inspector]
   returns: symbol"
  (comp u/load-param :returns))

(def type
  "Get the type of the field
   args: [inspector]
   returns: boolean"
  :type)

(extend-type Constructor Inspectable
  (inspect
    ([{:keys [flags parameter-types] :as c}]
     (map->ConstructorInspector {:name (:name c)  :visibility (u/visibility c)
                                 :flags flags     :params parameter-types}))))


(extend-type Method Inspectable
  (inspect
    ([{:keys [name flags return-type parameter-types] :as m}]
      (map->MethodInspector {:name  name           :visibility (u/visibility m)
                             :flags flags          :params parameter-types
                             :returns return-type  :static? (u/has-flag? m :static)}))))

(extend-type Field Inspectable
  (inspect
    ([{:keys [flags type] :as f}]
     (map->FieldInspector {:flags flags    :visibility (u/visibility f)  :type type
                           :name (:name f) :static?    (u/has-flag? f :static)}))))

(def compare-params (u/compare-with :params))

(defn -ctors [cs]
  (into (u/sorted-set-by compare-params) cs))

(defn -methods [ms]
  (->> (group-by :name ms)
       (into (sorted-map)
             (map (fn [[k v]] [k (into [] (sort compare-params v))])))))

(defn -fields [fs]
  (into (sorted-map) (map (fn [v] [(:name v) v])) fs))

(extend-type Class Inspectable
  (inspect [c]
    (let [{:keys [flags bases] :as c2} (r/reflect c)
          bs   (into (sorted-set) bases)
          mems (->> c2 :members (map inspect) (group-by class))
          cs   (-ctors   (mems ConstructorInspector))
          ms   (-methods (mems MethodInspector))
          fs   (-fields  (mems FieldInspector))]
      (map->ClassInspector {:constructors cs  :methods ms  :fields fs
                            :name (symbol (.getName c)) 
                            :bases bs  :flags flags
                            :static? (u/has-flag? c2 :static)}))))

(extend-type Object Inspectable
  (inspect [c]
    (inspect (class c))))
