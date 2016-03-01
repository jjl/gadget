(ns irresponsible.gadget.util-test
  (:require [irresponsible.gadget.util :as u]
            #?(:clj  [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is]]))
  (:import [irresponsible.gadget.util PrimitiveInspector ArrayInspector])
  ;;  #?(:cljs (:require [cljs.test :refer []])))
  (:refer-clojure :exclude [sorted-set-by]))

(defn id [& args]
  (into [] args))

(deftest load-class
  (is (= java.lang.Byte (u/load-class 'java.lang.Byte)))
  (is (nil? (u/load-class 'byte)))
  (is (nil? (u/load-class 'byte<>))))

(deftest load-param
  (let [r1 (u/load-param 'byte)
        r2 (u/load-param 'bytes)
        r3 (u/load-param 'byte<>)
        r4 (u/load-param 'java.lang.Byte<>)
        r5 (u/load-param 'java.lang.Byte)
        t1 (:type r1)
        t2 (:type r2)
        t3 (:type r3)
        t4 (:type r4)]
    (is (= java.lang.Byte (u/load-param 'java.lang.Byte)))
    (is (instance? PrimitiveInspector r1))
    (is (instance? PrimitiveInspector t2))
    (is (instance? PrimitiveInspector t3))
    (is (instance? ArrayInspector r2))
    (is (instance? ArrayInspector r3))
    (is (instance? ArrayInspector r4))
    (is (= 'byte t1))
    (is (= 'byte (:type t2)))
    (is (= 'byte (:type t3)))
    (is (= java.lang.Byte t4))
    (is (= java.lang.Byte r5))))

(deftest uncurry
  (is (= [:a :b]
         ((u/uncurry id) [:a :b]))))

(deftest assure
  (is (true? (u/assure true? true)))
  (is (false? (u/assure false? false)))
  (is (nil?  (u/assure true? false))))

(deftest score-visibility
  (is (= 1 (u/score-visibility :public)))
  (is (= 2 (u/score-visibility :protected)))
  (is (= 3 (u/score-visibility :private)))
  (is (= 4 (u/score-visibility nil))))

(deftest sorted-group-by
  (let [r (u/sorted-group-by odd? compare [1 2 3 4])]
    (is (sorted? r))
    (is (= r (into (sorted-map) {true [1 3]
                                 false [2 4]})))))

(deftest sorted-set-by
  (let [r (u/sorted-set-by compare [1 2 3 4])]
    (is (sorted? r))
    (is (set? r))
    (is (= #{1 2 3 4} (set r)))))

(deftest over-map
  (let [t {:a 1 :b 2}
        r (u/over-map map id t)]
    (is (= t r))))

(deftest over-vals
  (let [t {:a 1 :b 2}
        r (u/over-vals map inc t)
        e {:a 2 :b 3}]
    (is (= r e))))

(deftest map-vals
  (let [t {:a 1 :b 2}
        r1 (u/map-vals identity t)
        r2 (u/map-vals inc t)
        e {:a 2 :b 3}]
    (is (= t r1))
    (is (= r2 e))))

(deftest filter-vals
  (let [t {:a 1 :b 2}
        r1 (u/filter-vals (constantly true) t)
        r2 (u/filter-vals odd? t)
        e {:a 1}]
    (is (= t r1))
    (is (= e r2))))

(deftest to-string
  (is (= "foo" (u/to-string "foo")))
  (is (= "foo" (u/to-string :foo)))
  (is (= "foo" (u/to-string 'foo)))
  (is (= "" (u/to-string nil))))

(deftest str-concat
  (is (= "" (u/str-concat [])))
  (is (= "123" (u/str-concat [1 [2 3]])))
  (is (= ":a:b" (u/str-concat [:a :b]))))

(deftest classname
  (is (= "baz" (u/classname "foo.bar.baz")))
  (is (= "baz" (u/classname "foo.bar/baz")))
  (is (nil? (u/classname ""))))

(deftest make-name
  (is (= "a.b." (u/make-name ["a" "b"]))))

(deftest short-name
  (is (= "a.b." (u/short-name ["ab" "bc"]))))


(deftest shorten
  (let [in  ['java.lang.String 'java.lang.Byte 'java.lang.Character 'clojure.lang.PersistentVector
             'foo.bar 'java.lang.bar 'java.util.bar 'java.net.bar
             'java.nio.bar 'clojure.lang.bar 'clojure.core.bar
             'clojure.tools.bar 'clojure.foo.bar]
        out ['String 'Byte 'Character 'PersistentVector
             'foo.bar 'j.l.bar 'j.u.bar 'j.n.bar 'j.nio.bar 'c.l.bar 'c.c.bar 'c.t.bar 'c.foo.bar]]
    (is (= out (mapv u/shorten in)))))

(deftest maybe-shorten
  (is (= 'j.l.bar       (u/maybe-shorten 'java.lang.bar true)))
  (is (= 'java.lang.bar (u/maybe-shorten 'java.lang.bar false))))

(deftest get-params
  (is (= "[self String j.nio.bar]" (u/get-params {:params ['java.lang.String 'java.nio.bar]} {:shorten? true}))))

(deftest get-ctor-params
  (is (= "[String j.nio.bar]" (u/get-ctor-params {:params ['java.lang.String 'java.nio.bar]} {:shorten? true}))))

(deftest has-flag?
  (is (true? (u/has-flag? :public {:flags #{:public}})))
  (is (false? (u/has-flag? :public {:flags #{:private}}))))

(deftest visibility
  (doseq [[e t] [[:public #{:public}]  [:protected #{:protected}]  [:private #{:private}]
                 [:public #{:public :protected}]      [:public #{:public :private}]
                 [:protected #{:protected :private}]  [:public #{:public :protected :private}]
                 [nil #{:invalid}]]]
    (is (= e (u/visibility {:flags t})))))
