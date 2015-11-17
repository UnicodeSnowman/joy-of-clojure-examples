(ns joy-of-clojure.6-being-lazy)

; on immutability: being set in your ways

(def baselist (list :barnabas :adam))
(def lst1 (cons :willie baselist))
(def lst2 (cons :phoenix baselist))

(= (next lst1) (next lst2)) ; true, they're qual
(identical? (next lst1) (next lst2)) ; true, AND the exact same objects

; 6.2 Structural Sharing: a persistent toy

; a simple tree to help demonstrate how a tree can allow interior changes
; and maintain shared structure at the same time

(def a-tree {:val 5 :L nil :r nil})

(defn xconj [t v]
  (cond
    (nil? t) {:val v :L nil :R nil}
    (< v (:val t)) {:val (:val t)
                    :L (xconj (:L t) v)
                    :R (:R t)}
    :else          {:val (:val t)
                    :L (:L t)
                    :R (xconj (:R t) v)}))

; traverse the tree in sorted order, converting ot a seq to print succinctly
(defn xseq [t]
  (when t
    (concat (xseq (:L t)) [(:val t)] (xseq (:R t)))))

(concat [1] [2])

(def tree1 (xconj nil 5))
(def tree2 (xconj tree1 3))
(def tree3 (xconj tree2 2))
(def tree4 (xconj tree3 4))
(def tree5 (xconj tree4 8))
(xseq (:L tree5))
(identical? (:L tree4) (:L tree5)) ; => true
; values and unchanged branches are never copied
; but references to them are copied from nodes in the old tree to nodes in the new one

; 6.3 Laziness
(defn and-chain [x y z]
  (and x y z (do (println "Made it!") :all-truthy)))
(and-chain 1 2 3) ;
(and-chain 1 nil 3) ; => nil
(and-chain 1 false 3) ; => false

(def very-lazy (-> (iterate
                     #(do
                        (print \.)
                        (inc %)) 1)
                   rest
                   rest
                   rest))

(def less-lazy (-> (iterate
                     #(do
                        (print \.)
                        (inc %)) 1)
                   next
                   next
                   next))

(rest [1 2 3 4])
(next [1 2 3 4])

(defn arg-destruct [[x & xs]]
  (do
    (println x)
    (println xs)))
(arg-destruct [1 2 3 4 5])

(defn simple-range [i limit]
  (lazy-seq
    (when (< i limit)
      (cons i (simple-range (inc i) limit)))))
