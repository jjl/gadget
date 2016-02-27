(ns irresponsible.gadget
  (:require [clojure.reflect :as r]
            [clojure.string :as str]
            [irresponsible.gadget :as g]
            [irresponsible.gadget.util :as u]
            [classlojure.core :as c])
  (:import  [clojure.reflect Constructor Method Field]
            [clojure.lang PersistentVector PersistentArrayMap PersistentHashMap]
            [java.lang Class ClassLoader])
  (:refer-clojure :exclude [bases methods name type]))

(defn indent [t]
  (str "  " t))

(defn partial* [f & ps1]
  (fn [& ps2]
    (apply f (concat ps2 ps1))))

(defprotocol Summary
  (summary [insp] [insp opts]
    "Returns a short summary string"))

(defprotocol Inspectable
  (inspect [insp] [insp classloader]
    "Inspect the given thing"))

(defrecord PrimitiveInspector
  [type])

(defrecord ArrayInspector
  [type])

(defrecord ConstructorInspector
  [name visibility params])

(defrecord MethodInspector
  [name visibility static? params returns flags])

(defrecord FieldInspector
  [name visibility type static? flags])

(defrecord ClassInspector
  [bases constructors methods fields name static? flags])

(defn load-symbol [s]
  (letfn [(f [[t v :as r]]
            (case t
              :symbol    (if (var? v) @v v)
              :primitive (map->PrimitiveInspector {:type v})))]
    (let [[t v :as r] (u/load-symbol s)]
      (if (= t :array)
        (map->ArrayInspector {:type (f v)})
        (f r)))))

(def name
  "Gets the name of the inspected thing"
  :name)

(def visibility
  "one of: :public, :private, :protected, nil"
  :visibility)

(def public?
  "true if public"
  (comp (partial = :public)
        visibility))

(def protected?
  "true if protected"
  (comp (partial = :protected)
        visibility))

(def private?
  "true if private"
  (comp (partial = :private)
        visibility))

(def static?
  "Returns true if static"
  :static?)

(def dynamic?
  "Returns true if not static"
  (complement static?))

(defn bases
  "Returns a vector of ClassInspector for each base of the class
   Basic types are returned as symbols"
  [insp]
  (mapv (comp inspect load-symbol) (:bases insp)))

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
  "Get the vector of param types the invocable is invoked with"
  [insp]
  (->> insp :params (mapv load-symbol)))

(def returns
  "Get the return type of the invocable"
  (comp load-symbol :returns))

(def type "Get the type of the field" :type)

(extend-type PersistentVector Summary
  (summary
    ([^PersistentVector vec]
     (summary vec {}))
    ([vec opts]
     (mapv (partial* summary opts) vec))))

(extend-type PersistentArrayMap Summary
  (summary
    ([^PersistentArrayMap map]
     (summary map {}))
    ([map opts]
     (u/map-vals (partial* summary opts) map))))

(extend-type PersistentHashMap Summary
  (summary
    ([^PersistentHashMap map]
     (summary map {}))
    ([map opts]
     (u/map-vals (partial* summary opts) map))))

(extend-type PrimitiveInspector Summary
  (summary
    ([^PrimitiveInspector prim]
     (summary prim {}))
    ([{:keys [type]} opts]
     type)))

(extend-type ArrayInspector Summary
  (summary
    ([^PrimitiveInspector prim]
     (summary prim {}))
    ([{:keys [type]} opts]
     (str (summary type opts) "<>"))))

(extend-type ConstructorInspector Summary
  (summary
    ([^ConstructorInspector insp]
     (summary insp {}))
    ([{:keys [visibility name] :as insp}
      {:keys [shorten? in-class?] :or [nil nil nil] :as opts}]
     (let [v visibility
           params (u/get-ctor-params insp opts)
           name (u/maybe-shorten name shorten?)]
       (u/str-concat [(if in-class? "(" "(fn ")
                      (when (and v (not= :public v))
                        ["^" v " "])
                      (u/alias-name name) ". " params ")"])))))

(defn -summarise-constructors [cs {:keys [shorten? private?] :as opts}]
  (when-let [cs (seq (if private? cs (filter public? cs)))]
    (str (->> cs
              (into [(str "(" (u/alias-name (:name (first cs))) ".")]
                    (map  (partial* u/get-ctor-params opts)))
              (str/join " ")
              indent)
              ")")))

(extend-type MethodInspector Summary
  (summary
    ([^MethodInspector insp]
     (summary insp {}))
    ([{:keys [name returns] :as insp}
      {:keys [shorten? in-class?] :or [nil nil] :as opts}]
     (let [v (visibility insp)
           params (u/get-params insp (or opts {}))
           name (u/maybe-shorten name shorten?)
           returns (u/maybe-shorten returns shorten?)]
       (u/str-concat [(if in-class? "(" "(fn ")
                      (when (and v (not= :public v)) ["^" v " "])
                      (when (static? insp) "^:static ")
                      "^" returns " ." name " " params ")"])))))

(defn -summarise-method-group [name vis returns {:keys [shorten? private?] :as opts} ms]
  (when-let [ms (seq (if private? ms (filter public? ms)))]
    (let [first-line (-> ["(" (when (and vis (not= :public vis)) ["^" vis " "])
                          "^" (u/alias-name returns) " ." name]
                         u/str-concat)]
      (-> (->> ms
               (into [first-line] (map (partial* u/get-params opts)))
               (str/join " "))
          (str ")")))))

(defn -summarise-methods [ms {:keys [shorten? private?] :as opts}]
  (when-let [ms (-> ms seq vals flatten)]
    (->> (if private? ms (filter public? ms))
         (u/sorted-group-by (juxt u/score-visibility :name :returns))
         (reduce-kv (fn [acc _ ms]
                      (when (seq ms)
                        (let [{:keys [visibility name returns]} (first ms)]
                          (->> (-summarise-method-group name visibility returns opts ms)
                               indent
                               (conj acc)))))
                    []))))

(extend-type FieldInspector Summary
  (summary
    ([^FieldInspector insp]
     (summary insp {}))
    ([{:keys [name type visibility static?] :as insp}
      {:keys [shorten? in-class?] :or [nil nil]}]
     (let [v visibility
           name (u/maybe-shorten name shorten?)
           type (u/maybe-shorten type shorten?)]
       (u/str-concat [(when in-class? "(")
                      (when (and v (not= :public v))
                        ["^" v " "])
                      (when static? "^:static ")
                      "^" type " .-"  name
                      (when in-class? ")")])))))

(defn -summarise-fields [fs {:keys [shorten? private?] :as opts}]
  (when-let [fs (-> fs seq vals)]
    (->> (if private? fs (filter public? fs))
         (map (comp indent (partial* summary (assoc opts :in-class? true)))))))

(defn -summarise-bases [bs {:keys [shorten?] :as opts}]
  (->> bs
       (mapv (comp (partial* u/maybe-shorten shorten?)))
       (str/join " ")
       indent))

(extend-type ClassInspector Summary
  (summary
    ([^ClassInspector insp]
     (summary insp {}))
    ([{:keys [name] :as insp}
      {:keys [shorten?] :or [false] :as opts}]
     (let [bs (-> insp :bases       (-summarise-bases        opts))
           cs (-> insp constructors (-summarise-constructors opts))
           ms (-> insp methods      (-summarise-methods      opts))
           fs (-> insp fields       (-summarise-fields       opts))]
       (str (str/join "\n" (filter #(and (not (nil? %)) (not (= "" %)))
                                   (flatten [(str "(deftype+ " name)
                                             bs cs ms fs])))
            ")")))))

(extend-type Constructor Inspectable
  (inspect
    ([^Constructor c]
     (inspect c c/base-classloader))
    ([{:keys [flags parameter-types] :as c} ^ClassLoader cl]
     (map->ConstructorInspector
      {:name (:name c)  :visibility (u/visibility c)
       :flags flags     :params parameter-types}))))


(extend-type Method Inspectable
  (inspect
    ([^Method m]
     (inspect m c/base-classloader))
    ([{:keys [name flags return-type parameter-types] :as m} ^ClassLoader cl]
      (map->MethodInspector {:name  name          :visibility (u/visibility m)
                            :flags flags          :params parameter-types
                            :returns return-type  :static? (u/has-flag? m :static)}))))

(extend-type Field Inspectable
  (inspect
    ([^Field f]
     (inspect f c/base-classloader))
    ([{:keys [flags type] :as f} ^ClassLoader cl]
     (map->FieldInspector {:flags flags    :visibility (u/visibility f)  :type type
                           :name (:name f) :static?    (u/has-flag? f :static)}))))

(extend-type Class Inspectable
  (inspect [c]
    (let [{:keys [flags bases] :as c2} (r/reflect c)
          mems (->> c2 :members
                    (map inspect)
                    (group-by class))
          cs   (->> ConstructorInspector mems
                    (into (u/sorted-set-by (comp count :params))))
          ms   (->> MethodInspector mems
                    (u/sorted-group-by :name))
          fs   (->> FieldInspector mems
                    (into (sorted-map) (map (fn [v] [(:name v) v]))))]
      (map->ClassInspector {:constructors cs  :methods ms  :fields fs
                            :name (symbol (.getName c)) 
                            :bases bases  :flags flags
                            :static? (u/has-flag? c2 :static)}))))
