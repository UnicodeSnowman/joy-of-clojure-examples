(ns joy-of-clojure.7-functional-programming)

; vectors are functions of their indices
(map [:chton :phtor :beowulf :grendel] #{0 3}) ; => (:chton :grendel) as a seq

; composing functions
((comp rest rest rest) [1 2 3 4])

(defn fnth [n]
  (apply comp ; apply, similar to JS apply
         (cons first
               (take (dec n) (repeat rest))))) ; repeatedly call rest, reducing our list until
                                               ; it contains one element, then call first on that
                                               ; single element list
((fnth 5) '[a b c d e])

(apply (fn [v & other] other) [1 2 3 4 5])

(map (comp
       keyword
       #(.toLowerCase %)
       name)
     '(a B C)) ; => (:a :b :c)

; partial functions
((partial + 5) 100 200) ; => 305

; `complement` returns a function that takes a value and returns the opposite
((complement (fn [v] v)) true)
((complement (fn [v] v)) nil)

; 7.1.2 Higher-order functions

; sort-by !!!
; sort fails if given seqs containing non-mutually comparable elements,
; also fails if comparing sub elements is the intention
; sort-by is the solution... accepts a fn to preprocess elements
; into something that is comparable
(sort-by second [[:a 7] [:c 13] [:b 21]])
(sort-by str ["z" "x" "a" "aa" 1 5 8])
(sort-by :age [{:age 99} {:age 13} {:age 71}])

; allows for powerful sorting of vectors of maps and such
(def plays [{:band "Burial" :plays 979 :loved 9}
            {:band "Eno" :plays 2333 :loved 15}
            {:band "Bill Evans" :plays 979 :loved 9}
            {:band "Magma" :plays 2665 :loved 31}])
(def sort-by-loved-ratio (partial sort-by #(/ (:plays %) (:loved %))))
(sort-by-loved-ratio plays)

; functions as return values
(defn columns [column-names]
  (fn [row] ; returns a fn that accepts a row
    (vec (map row column-names)))) ; here, calling the `row` hash as a fn

(sort-by (columns [:plays :loved :band]) plays) ; defines sorting as a list of priorities... backup sorting keys will be used if identical values are provided for a given key

; (columns [:plays...]) returns a function used as the `sort-by` fn
; this function is called an every index of `plays`

; 7.1.3 Pure Functions
; referential transparency! i.e. the reference to the function is transparent to time, time has no effect on the fn call
(zipmap [:one :two :three] [1 2 3])

(defn keys-apply [f ks m]
  (let [only (select-keys m ks)]
    (zipmap (keys only)
            (map f (vals only)))))

(plays 0)
