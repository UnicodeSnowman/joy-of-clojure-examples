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

; apply a fn to each value of the corresponding seq of keys
(keys-apply #(.toUpperCase %) #{:band} (plays 0))
(keys-apply #(int (/ % 2)) #{:plays :loved} (plays 0))

(defn manip-map [f ks m]
  (merge m (keys-apply f ks m))) ; keeps existing keys/values in m not targeted in `ks`

(manip-map #(int (/ % 2)) #{:plays :loved} (plays 0))

; 7.1.4 Named Arguments
(defn slope
  [& {:keys [p1 p2] :or {p1 [0 0] p2 [1 1]}}]
  (float (/ (- (p2 1) (p1 1))
            (- (p2 0) (p1 0)))))

(slope :p1 [4 15] :p2 [3 21]) ; can pass arguments as keys...
(slope :p2 [2 1])
(slope [4 15] [3 21]) ; or as individual fn arguments
(slope)

; 7.1.5 pre and post conditions
; functions can be constrained to specific argument conditions and return
; value conditions via... 
(defn slope-with-conditions [p1 p2]
  {:pre [(not= p1 p1) (vector? p1) (vector? p2)]
   :post [(float? %)]}
  (float (/ (- (p2 1) (p1 1))
            (- (p2 0) (p1 0)))))
(slope-with-conditions [10 10] [10 10])
; apparently there's more on this later in the book. really
; cool idea though

; can also pull constraints out into individual "wrapper" functions
(defn vegan-constraints [f m] ; accepts a function to check with conditions, then calls it as normal
  {:pre [(:veggie m)]
   :post [(:veggie %) (nil? {:meat %})]}
  (f m))

; 7.2 On Closures

; quintessential example
(defn times-n [n]
    (fn [y] (* y n)))

((times-n 5) 2)

(defn filter-divisible [denom s]
  (filter #(zero? (rem % denom)) s))
(filter-divisible 5 (range 20))

; 7.2.4 Sharing Closure Context
(def bearings [{:x  0, :y  1}    ; north
               {:x  1, :y  0}    ; east
               {:x  0, :y -1}    ; south
               {:x -1, :y  0}])  ; west

(defn forward [x y bearing-num]
  [(+ x (:x (bearings bearing-num)))
   (+ y (:y (bearings bearing-num)))])

(forward 5 5 0) ; move north from (5, 5)

; bot keeps track of position and direction, essentially creates instances
; when calling it...
(defn bot [x y bearing-num]
  {:coords [x y]
   :bearing ([:north :east :south :west] bearing-num)
   :forward (fn [] (bot (+ x (:x (bearings bearing-num)))
                        (+ y (:y (bearings bearing-num)))
                        bearing-num))
   :turn-right (fn [] (bot x y (mod (+ 1 bearing-num) 4)))
   :turn-left (fn [] (bot x y (mod (- 1 bearing-num) 4)))})

(def my-bot (bot 5 5 0)) ; bot instance
(:coords my-bot)
(:bearing my-bot)
(:coords ((:forward my-bot))) ; extra set of parens because :forward returns a fn

; 7.3 Thinking Recursively

(defn pow [base exp] ; explicit recursion without `recur` keyword... can blow stack
  (if (zero? exp) 1
    (* base (pow base (dec exp)))))

(pow 2 10)

(defn pow-no-overflow [base exp]
  (letfn [(kapow [base exp acc]
            (if (zero? exp)
              acc
              (recur base (dec exp) (* base acc))))] ; tail recursive
    (kapow base exp 1)))

(pow-no-overflow 2 10)

; 7.3.2 Tail Calls and Recur
; how do tail calls work? if fn A calls fn B (in tail position), all A resources
; are deallocated and delegated to execution context of B. As a result of this 
; optimization, return to original caller is direct, rather than back down the
; call chain through A
; ... but apparently clojure doesn't offer full tail-call optimization, only
; self-call tail-call optimization. this is caused by the JVM, apparently.
; using `recur` will prevent stack overflow

(defn gcd [x y]
   (cond
     (> x y) (recur (- x y) y)
     (< x y) (recur x (- y x))
     :else x))

; "Why Recur?" section is really great... I should review this later

; 7.3.3 Trampoline?
; `let-fn`: allows you to create local functions that reference each other, unlike `let`
; each state function returns a function (that returns a value) in order to support
; `trampoline`, which can then manage the stack on mutually recursive calls, thus
; avoiding overflow

(defn elevator [commands]
  (letfn [
    (ff-open [[_ & r]]
      ""
      #(case _
        :close (ff-closed r)
        :done true
        false))
    (ff-closed [[_ & r]]
      ""
      #(case _
        :open (ff-open r)
        :up (sf-closed r)
        false))
    (sf-closed [[_ & r]]
      ""
      #(case _
        :down (ff-closed r)
        :open (sf-open r)
        false))
    (sf-open [[_ & r]]
      ""
      #(case _
        :close (sf-closed r)
        :done true
        false))]
    (trampoline ff-open commands))) ; sweeeeeeeet... keep calling chain of
; recursive functions without blowing our stack in the event of a set of
; deeply nested calls
; - make all functions participating in mutual recursion return a function instead
;   of their normal result
; - invoke the first function in the mutual chain via the `trampoline` fn

(elevator [:close :open :close :up :open :open :done])
(elevator [:close :up :open :close :down :open :done])
; handles recursive calls explicitly, no stack overflow
; (elevator (cycle [:close: open])) ; runs forever

; 7.4 A* pathfinding
(def world [[  1   1   1   1   1]
            [999 999 999 999   1]
            [  1   1   1   1   1]
            [  1 999 999 999 999]
            [  1   1   1   1   1]])

(defn estimate-cost [step-cost-est size y x]
  (* step-cost-est
     (- (+ size size) y x 2)))

(estimate-cost 900 5 0 0)
(estimate-cost 900 5 4 4)

(defn path-cost [node-cost cheapest-nbr]
  (+ node-cost
     (or (:cost cheapest-nbr) 0)))

(path-cost 900 {:cost 1})
(path-cost 900 {})

(defn total-cost [newcost step-cost-est size y x]
  (+ newcost
     (estimate-cost step-cost-est size y x)))

(defn min-by [f coll]
  (when (seq coll)
    (reduce (fn [min other]
              (if (> (f min) (f other))
                other
                min))
            coll)))

(min-by :cost [{:cost 29} {:cost 31} {:cost 10}])
(sorted-set :cost :zzz :abc)

(#(let [[_  y :as something] (first [[1 2 3]])]
    y))

(disj #{1 2 3} 3)

; long astar algorithm... need to come back to this
; (defn astar [start-yx step-est cell-costs]
;   (let [size (count cell-costs)]
;     (loop [steps 0
;            routes (vec (replicate size (vec (replicate size nil))))
;            work-todo (sorted-set [0 start-yx])]
;       (if (empty? work-todo)
;         [(peek (peek routes)) :steps steps]
;         (let [...])))))

