(ns joy-of-clojure.core
  (:require [clojure.set :refer (intersection union)]))

; PersistentQueue
(defmethod print-method clojure.lang.PersistentQueue
  [q, w]

  (print-method '<- w)
  (print-method (seq q) w)
  (print-method '-< w))

(def schedule
  (conj clojure.lang.PersistentQueue/EMPTY
        :wake-up :shower :brush-teeth))

(peek schedule) ; returns head
(rest schedule) ; returns seq
(pop schedule)  ; returns clojure.lang.PersistentQueue

; sets
(get #{:a 1 :b 2} :c :derp)

; checking for a key in a set (contains doesn't work as expected)
(some #{1 :b} [:a 1 :b 2])

(sorted-set :b :c :a)

; clojure.set
(intersection #{:humans :fruit-bats :zombies}
              #{:chupacabra :zombies :humans})

(union #{:humans :fruit-bats :zombies}
       #{:chupacabra :zombies :humans})

; thinking in maps
(def m (hash-map :a 1 :b [1 2 3] :c 3 :d 4 :e 5))
(get m :b "NOPE")
(seq m) ; => ([:e 5] [:c 3] ...) sequence of map entries
(into {} [[:a 1] '[:b 2]]) ; ...and the reverse
(into {} (map vec '[(:a 1) (:b 2)])) ; ...or from non-vectors (i.e. lists)
(apply hash-map [:a 1 :b 2]) ; don't even need to be grouped, can use apply
(zipmap [:a :b] [1 2]) ; or zip 'em up

; sorted maps
(sorted-map :thx 1138 :r2d 2) ; default, sorted by keys
(sorted-map-by
  #(compare (subs %1 1) (subs %2 1))
  "bac" 2 "abc" 9) ; custom sorting

; array maps (keeping insertion order)
(seq (array-map :a 1 :b 2 :c 3))

; Finding the Position of Items in a Sequence!
(defn index [coll] ; helper fn: generate a uniform representation of indexed collections
  (cond
    (map? coll) (seq coll)
    (set? coll) (map vector coll coll)
    :else (map vector (iterate inc 0) coll))) ; iterate through infinite sequence assigning index for each

; (index [:a 1 :b 2 :c 3 :d 4])
; (index {:a 1 :b 2 :c 3 :d 4})
; (index #{:a 1 :b 2 :c 3 :d 4})

; basic version
(defn pos [target-index coll]
  (for [[i v] (index coll) :when (= target-index v)] i))

(pos 3 [:a 1 :b 2 :c 3 :d 4])
(pos 3 [:a 3 :b 3 :c 3 :d 4])
(pos 3 {:a 3 :b 3 :c 3 :d 4})

; can improve... supply predicate
(defn pos-pred [predicate coll]
  (for [[i v] (index coll) :when (predicate v)] i))

(pos-pred #{3 4} {:a 1 :b 2 :c 3 :d 4})
(pos-pred even? [1 2 3 4])
