(ns irresponsible.gadget.jar-test
  (:require [irresponsible.gadget.jar :as j]
            [irresponsible.unscrew :as unscrew]
            #?(:clj  [clojure.test :refer [deftest is]]
               :cljs [cljs.test :refer [deftest is]])))
;;  #?(:cljs (:require [cljs.test :refer []])))
  
(deftest normalisation
  (let [classes-in  ["foo/bar.class"]
        classes-out ["foo.bar"]
        clojure-in  ["foo/bar_baz.clj" "foo/bar_baz.cljs" "foo/bar_baz.cljc"]
        clojure-out ["foo.bar-baz" "foo.bar-baz" "foo.bar-baz"]]
    (is (= classes-out (mapv j/normalise-class   classes-in)))
    (is (= clojure-out (mapv j/normalise-namespace clojure-in)))))

(defn -matching [jar pred]
  [jar (fn? pred)])

(deftest entries-matching
  (with-redefs [unscrew/entries-matching -matching]
    (is (= [:foo true]
           (j/namespaces-in-jar :foo)))
    (is (= [:foo true]
           (j/classes-in-jar :foo)))))
