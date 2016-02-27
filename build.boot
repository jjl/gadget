(set-env!
  :project 'irresponsible/gadget
  :version "0.0.1"
  :resource-paths #{"src"}
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0"                  :scope "provided"]
                  [org.clojure/tools.reader "1.0.0-alpha3"] ;; should probably just copy-paste the fn
                  [classlojure "0.6.6" :exclusions [org.clojure/clojure]]
                  ;; [automat     "0.2.0-alpha2"]
                  [adzerk/boot-test "1.1.0"                     :scope "test"]
                  ;; [org.clojure/clojurescript "1.7.228"          :scope "test"]
                  ;; [adzerk/boot-cljs "1.7.228-1"                 :scope "test"]
                  ;; [adzerk/boot-cljs-repl       "0.3.0"          :scope "test"]
                  ;; [adzerk/boot-reload          "0.4.5"          :scope "test"]
                  ;; [pandeiro/boot-http          "0.7.1-SNAPSHOT" :scope "test"]
                  ;; [com.cemerick/piggieback     "0.2.1"          :scope "test"]
                  ;; [weasel                      "0.7.0"          :scope "test"]
                  [org.clojure/tools.nrepl     "0.2.12"         :scope "test"]
                  ;; [crisptrutski/boot-cljs-test "0.2.2-SNAPSHOT" :scope "test"]
                  ])

(require '[adzerk.boot-test :as boot-test])
         ;; '[adzerk.boot-cljs :as boot-cljs]
         ;; '[adzerk.boot-cljs-repl :as boot-cljs-repl]
         ;; '[adzerk.boot-reload    :as boot-reload]
         ;; '[crisptrutski.boot-cljs-test :as boot-cljs-test]
         ;; '[pandeiro.boot-http :as boot-http])
         
(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)
       :description "Some introspection helpers that are useful for REPL work"
       :url "https://github.com/irresponsible/frost"
       :scm {:url "https://github.com/irresponsible/frost.git"}
       :license {"MIT" "https://en.wikipedia.org/MIT_License"}}
  target  {:dir #{"target"}})

(deftask test-clj []
  (set-env! :source-paths #(conj % "test"))
  (comp (target) (speak) (boot-test/test)))

;; (deftask test-cljs []
;;   (set-env! :source-paths #(conj % "test"))
;;   (comp (target) (speak) (boot-cljs/cljs) (boot-cljs-test/test-cljs)))
  
(deftask test []
  (set-env! :source-paths #(conj % "test"))
  (comp (target) (speak) (boot-test/test)))

(deftask autotest []
  (comp (watch) (test)))

(deftask make-release-jar []
  (comp (target) (pom) (jar)))
