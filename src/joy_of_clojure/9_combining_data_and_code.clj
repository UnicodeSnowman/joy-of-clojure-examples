(ns joy-of-clojure.9-combining-data-and-code)

; 9.1 Namespaces
; joy.ns/authors caused two-level lookup... a symbol `joy.ns` is
; used to look up a namespace map, and a symbol `authors` is used
; to retrieve a var
(in-ns 'joy.ns)
(def authors ["Chouser"])

(in-ns 'your.ns)
(clojure.core/refer 'joy.ns)
(clojure.core/prn joy.ns/authors) ; how to import prn to this namespace?

(in-ns 'joy.ns)
(def authors ["Chouser" "Fogus"])

(in-ns 'your.ns)
(clojure.core/prn joy.ns/authors)

; 9.1.3 Declarative inclusions and exclusions

(ns joy.ns-ex
  (:refer-clojure :exclude [defstruct]) ; exclude defstruct from clojure.core
  (:use (clojure set xml)) ; use everything in clojure.set and clojure.xml w/o qualification
  (:use [clojure.test :only (are is)])
  (:require (clojure [zip :as z]))
  (:import (java.util Date)
           (java.io File)))

(def assoc-thing (assoc {:something 123} ::prototype 555))
(::prototype assoc-thing)
(find assoc-thing :other)
(find assoc-thing :something)

(do
  (let [[a b] (find assoc-thing :something)]
    (prn a)
    (prn b)))

; 9.3 Types, protocols, and records

; Records
(defrecord TreeNode [val l r])
(def my-node (TreeNode. 5 nil nil))
(:val my-node)
(get my-node :val "NOPE")

(defn xconj [t v]
  (cond
    (nil? t) (TreeNode. v nil nil)
    (< v (:val t)) (TreeNode. (:val t) (xconj (:l t) v) (:r t))
    :else (TreeNode. (:val t) (:l t) (xconj (:r t) v))))

(defn xseq [t]
  (when t
    (concat (xseq (:l t)) [(:val t)] (xseq (:r t)))))

(def sample-tree
  (reduce xconj nil [3 5 2 4 6]))
(xseq sample-tree)

(dissoc (TreeNode. 5 nil nil) :l) ; => returns a regular map

; Protocols
(defprotocol FIXO
  (fixo-push [fixo value])
  (fixo-pop [fixo])
  (fixo-peek [fixo]))

(extend-type TreeNode
  FIXO
  (fixo-push [node value]
    (xconj node value)))

(xseq (fixo-push sample-tree 5/2))
(extend-type clojure.lang.IPersistentVector
  FIXO
  (fixo-push [vector value]
    (conj vector value)))

; mixins
; extend is ALWAYS about a protocol
(defprotocol StringOps (rev [s]) (upp [s]))
(def rev-mixin {:rev clojure.string/reverse})
(def upp-mixin {:upp (fn [this] (.toUpperCase this))})
(def fully-mixed (merge upp-mixin rev-mixin))
(extend String StringOps fully-mixed)
(-> "Works" upp rev)

; can also include protocol implementation inline with defrecord
(defrecord TreeNode [val l r]
  FIXO
  (fixo-push [t v]
    ...)
  (fixo-peek [t]
    ...)
  (fixo-pop [t]
    ...))

; for lower level consruct overrides:
(deftype InfiniteConstant [i]
  clojure.lang.ISeq
  (seq [this]
    (lazy-seq (cons i (seq this)))))

(take 3 (InfiniteConstant. 5))

; 9.4 a fluent builder for chess moves

(defn build-move [& pieces]
  (apply hash-map pieces))

(build-move :from "e7" :to "e8" :promotion \Q)

(defrecord Move [from to castle? promotion]
  Object
  (toString [this]
    (str "Move " (:from this)
         " to " (:to this)
         (if (:castle? this) " castle"
           (if-let [p (:promotion this)]
             (str " promote to " p))))))

(str (Move. "e2" "e4" nil nil))
(str (Move. "e2" "e4" true nil))
(str (Move. "e2" "e4" nil \Q))

(.println System/out (Move. "e7" "e8" nil \Q))

; applying conditions
(defn build-move [& {:keys [from to castle? promotion]}]
  {:pre [from to]}
  (Move. from to castle? promotion))

(str (build-move :from "e2" :to "e4"))
(str (build-move :to "e2" :from "e4"))
(str (build-move :from "e2" :derp "e4"))
