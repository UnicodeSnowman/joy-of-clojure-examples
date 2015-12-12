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
(get my-node get my-node :val "NOPE")

