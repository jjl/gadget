(ns irresponsible.gadget.summary
  (:require [clojure.string :as str]
            [irresponsible.gadget.util :as u :refer [partial*]]
            [irresponsible.gadget.inspector :as i])
  (:import  [clojure.reflect Constructor Method Field]
            [clojure.lang PersistentVector PersistentArrayMap PersistentHashMap]
            [irresponsible.gadget.inspector ConstructorInspector MethodInspector FieldInspector ClassInspector]
            [irresponsible.gadget.util PrimitiveInspector ArrayInspector]))
  
(defprotocol Summary
  (summary [insp] [insp opts]
    "Returns a short summary string
     args: [insp] [insp opts]
       opts: map of options. supported keys:
         :shorten?  when true, shorten common namespaces (default: true)
         :private?  when true, show private and protected members (default: false)
     returns: string"))

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
  (when-let [cs (seq (if private? cs (filter i/public? cs)))]
    (let [nom (u/classname (:name (first cs)))
          pad (+ 5 (.length nom))
          [car & cdr :as cs2] (map (partial* u/get-ctor-params opts) cs)
          car2 (str "  (" nom ". " car)
          cdr2 (map (partial u/indent pad) cdr)
          lines (into ["  ;; constructors" car2] cdr2)]
      (str (str/join "\n" lines) ")"))))

(extend-type MethodInspector Summary
  (summary
    ([^MethodInspector insp]
     (summary insp {}))
    ([{:keys [name returns] :as insp}
      {:keys [shorten? in-class?] :or [nil nil] :as opts}]
     (let [v (:visibility insp)
           params (u/get-params insp (or opts {}))
           name (u/maybe-shorten name shorten?)
           returns (u/maybe-shorten returns shorten?)]
       (u/str-concat [(if in-class? "(" "(fn ")
                      (when (and v (not= :public v)) ["^" v " "])
                      (when (:static? insp) "^:static ")
                      "^" returns " ." name " " params ")"])))))

(defn -subgroup
  ""
  [ms]
  (let [rets (group-by :returns ms)]
    (map rets (sort (keys rets)))))

(defn -summarise-subgroup [{:keys [private? shorten] :as opts} s]
  (let [s2 (cond->> s private? (filter i/public?))]
    (when (seq s2)
      (let [[{:keys [visibility returns name] :as s1} & ss] s2
            vis (if (and visibility (not= :public visibility)) (str "^" visibility " ") "")
            rets (u/classname returns)
            ann  (str "  (" vis  "^" rets " ." name " ")
            pad  (.length ann)
            [o1 & os] (map #(u/get-params % opts) s2)
            line-1 (str ann o1)
            os2 (map (partial u/indent pad) os)]
        (str (str/join "\n" (cons line-1 os2)) ")")))))

(defn -summarise-group
  "A method group shares the same name"
  [opts ms]
  (->> (-subgroup ms) (keep (partial -summarise-subgroup opts))))

(defn -summarise-methods [ms opts]
  (->> (vals ms)
       (keep (partial -summarise-group opts))
       (into ["  ;; methods"])))

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
    (->> (if private? fs (filter i/public? fs))
         (map (comp (partial u/indent 2) (partial* summary (assoc opts :in-class? true))))
         (cons "  ;; fields"))))

(defn -summarise-bases
  "[internal]
   Turns a vector of symbols into a list of bases suitable for embedding in class output
   args: [fs opts]
     opts: map of options (:shorten? :private?)
   returns: String"
  [bs {:keys [shorten?] :as opts}]
  (->> bs
       (mapv (comp (partial* u/maybe-shorten shorten?)))
       pr-str
       (str "  :bases ")))

(extend-type ClassInspector Summary
  (summary
    ([^ClassInspector insp]
     (summary insp {}))
    ([{:keys [name] :as insp} opts]
     (let [bs (-> insp :bases        (-summarise-bases        opts))
           cs (-> insp :constructors (-summarise-constructors opts))
           ms (-> insp :methods      (-summarise-methods      opts))
           fs (-> insp :fields       (-summarise-fields       opts))]
       (str (str/join "\n" (remove #(or (nil? %) (= "" %))
                                   (flatten [(str "(deftype++ " name)
                                             bs cs ms fs])))
            ")")))))
