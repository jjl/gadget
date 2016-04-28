(ns irresponsible.gadget-test
  (:require [clojure.test :refer [deftest is]]
            [irresponsible.gadget :as g])
  (:import [irresponsible.gadget ConstructorInspector MethodInspector FieldInspector ClassInspector])
  (:refer-clojure :exclude [name bases methods type]))

(defn id [& args]
  (into [] args))

(deftest partial*
  (let [f (g/partial* id 1 2 3)]
    (is (fn? f))
    (is (= [4 5 6 1 2 3]
           (f 4 5 6)))))

(deftest summary)

(deftest inspect
  (let [insp (g/inspect java.lang.String)]
    (is (instance? ClassInspector insp))))

(deftest gadget
  (is (= (g/summary (g/inspect java.lang.String) {:shorten? true})
         (g/gadget java.lang.String))))

(deftest wtf?
  (= (g/gadget String)
     (with-out-str (g/wtf? String))))

(deftest name
  (is (= :foo (g/name {:name :foo}))))

(deftest visibility
  (is (= :foo (g/name {:name :foo}))))

(deftest public?
  (is (= true  (g/public? {:visibility :public})))
  (is (= false (g/public? {:visibility :protected})))
  (is (= false (g/public? {:visibility :private}))))

(deftest protected?
  (is (= false (g/protected? {:visibility :public})))
  (is (= true  (g/protected? {:visibility :protected})))
  (is (= false (g/protected? {:visibility :private}))))

(deftest private?
  (is (= false (g/private? {:visibility :public})))
  (is (= false (g/private? {:visibility :protected})))
  (is (= true  (g/private? {:visibility :private}))))

(deftest static?
  (is (= true (g/static? {:static? true})))
  (is (nil?   (g/static? {}))))

(deftest dynamic?
  (is (= false (g/dynamic? {:static? true})))
  (is (= true  (g/dynamic? {}))))

(let [insp (g/inspect java.lang.String)
      bs (g/bases insp)
      cs (g/constructors insp)
      fs (g/fields insp)
      ms (g/methods insp)
      m1 (first ms)
      ps (g/params m1)
      rs (g/returns m1)]

  (deftest bases
    (is (vector? bs))
    (is (every? (partial instance? ClassInspector) bs)))

  (deftest constructors
    (is (set? cs))
    (is (every? (partial instance? ConstructorInspector) cs)))

  (deftest fields
    (is (map? fs))
    (is (every? (partial instance? FieldInspector) (vals fs))))

  (deftest methods
    (is (map? ms))
    (is (every? #(every? (partial instance? MethodInspector) %) (vals ms))))

  (deftest params
    (is (every? symbol? ps))))
  
  ;; (deftest returns
  ;;   (is (= java.lang.String rs))))

;; (deftest type)

;; (deftest -summarise-constructors)

;; (deftest -summarise-method-group)

;; (deftest -summarise-methods)

;; (deftest -summarise-fields)

;; (deftest -summarise-bases)
