(ns irresponsible.gadget
  (:require [clojure.reflect :as r]
            [clojure.string :as str]
            [irresponsible.gadget.util :as u])
  (:import  [clojure.reflect Constructor Method Field]
            [clojure.lang PersistentVector PersistentArrayMap PersistentHashMap]
            [irresponsible.gadget.util PrimitiveInspector ArrayInspector])
  (:refer-clojure :exclude [bases methods name type]))

(defn- indent
  "Indents the given token:
   args: [string]
   returns: string"
  [t]
  (str "  " t))

(defn partial*
  "Like partial, but appends the provided arguemnts to the END of the received
   arguments in the return function
   args: [f & ps]
   returns: function"
  [f & ps1]
  (fn [& ps2]
    (apply f (concat ps2 ps1))))

(defprotocol Summary
  (summary [insp] [insp opts]
    "Returns a short summary string
     args: [insp] [insp opts]
       opts: map of options. supported keys:
         :shorten?  when true, shorten common namespaces
         :private?  when true, show private and protected members
     returns: string"))

(defprotocol Inspectable
  (inspect [insp]
    "Inspect the given thing, returning some sort of Inspector
     args: [thing]"))

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
    ([^ArrayInspector prim]
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
                      (u/classname) ". " params ")"])))))

(defn -summarise-constructors [cs {:keys [shorten? private?] :as opts}]
  (when-let [cs (seq (if private? cs (filter public? cs)))]
    (str (->> cs
              (into [(str "(" (u/classname (:name (first cs))) ".")]
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
                          "^" (u/classname returns) " ." name]
                         u/str-concat)]
      (-> (->> ms
               (into [first-line] (map (partial* u/get-params opts)))
               (str/join " "))
          (str ")")))))

(defn -summarise-methods [ms {:keys [shorten? private?] :as opts}]
  (when-let [ms (-> ms seq vals flatten)]
    (->> (if private? ms (filter public? ms))
         (u/sorted-group-by :name (juxt u/score-visibility :name (comp count :returns)))
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

(defn -summarise-fields
  "[internal]
   Turns a field map into a sequence of field summaries suitable for embedding
   in class output
   args: [fs opts]
     opts: map of options (:shorten? :private?)
   returns: [String]"
  [fs {:keys [shorten? private?] :as opts}]
  (when-let [fs (-> fs seq vals)]
    (->> (if private? fs (filter public? fs))
         (map (comp indent (partial* summary (assoc opts :in-class? true)))))))

(defn -summarise-bases
  "[internal]
   Turns a vector of symbols into a list of bases suitable for embedding in class output
   args: [fs opts]
     opts: map of options (:shorten? :private?)
   returns: String"
  [bs {:keys [shorten?] :as opts}]
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
       (str (str/join "\n" (remove #(or (nil? %) (= "" %))
                                   (flatten [(str "(deftype+ " name)
                                             bs cs ms fs])))
            ")")))))

(extend-type Constructor Inspectable
  (inspect
    ([{:keys [flags parameter-types] :as c}]
     (map->ConstructorInspector
      {:name (:name c)  :visibility (u/visibility c)
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

(extend-type Class Inspectable
  (inspect [c]
    (let [{:keys [flags bases] :as c2} (r/reflect c)
          mems (->> c2 :members
                    (map inspect)
                    (group-by class))
          cs   (->> ConstructorInspector mems
                    (into (u/sorted-set-by (comp count :params))))
          ms   (->> MethodInspector mems
                    (u/sorted-group-by :name #(compare (:name %) (:name %2))))
          fs   (->> FieldInspector mems
                    (into (sorted-map) (map (fn [v] [(:name v) v]))))]
      (map->ClassInspector {:constructors cs  :methods ms  :fields fs
                            :name (symbol (.getName c)) 
                            :bases bases  :flags flags
                            :static? (u/has-flag? c2 :static)}))))
