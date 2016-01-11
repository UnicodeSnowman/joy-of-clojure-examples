(ns joy-of-clojure.11-parallelism
  (:require (clojure.core [reducers :as r]))
  (:require (clojure [xml :as xml]))
  (:require (clojure [zip :as zip]))
  (:import (java.util.regex.Pattern))
  (:import java.util.concurrent.Executors))


(def thread-pool
  (Executors/newFixedThreadPool
    (+ 2 (.availableProcessors (Runtime/getRuntime)))))

(defn dothreads!
  [f & {thread-count :threads
        exec-count   :times
        :or {thread-count 1 exec-count 1}}]
  (dotimes [t thread-count]
    (.submit thread-pool
             #(dotimes [_ exec-count] (f)))))
; when to use futures
; when to use promises
; parallel operations
; introduction to reducer/fold

; 11.1 when to use futures
(time (let [x (future (do (Thread/sleep 5000) (+ 41 1)))]
        [@x @x]))

(defmacro as-futures [[a args] & body]
  (let [parts (partition-by #{'=>} body)
        [acts _ [res]] (partition-by #{:as} (first parts))
        [_ _ task] parts]
    `(let [~res (for [~a ~args] (future ~@acts))]
       ~@task)))

(defn occurrences [extractor tag & feeds]
  (as-futures [feed feeds]
    (count-text-task extractor tag feed)
    :as results
    =>
    (reduce + (map deref results))))

(def a (future 123))
(prn @a)

; 11.2 when to use promises
(def x (promise))
(def y (promise))
(def z (promise))

(dothreads! #(deliver z (+ @x @y)))

(dothreads!
  #(do (Thread/sleep 2000) (deliver x 52)))

(dothreads!
  #(do (Thread/sleep 4000) (deliver x 86)))

(time @z)

; 11.3.1 pvalues
; analagous to as-futures, executes arbitrary number of expressions in parallel

(pvalues 1 2 (+ 1 2))

; NOTE return type is a lazy sequence

(defn sleeper [s thing] (Thread/sleep (* 1000 s)) thing)
(defn pvs [] (pvalues
               (sleeper 2 :1st)
               (sleeper 3 :2nd)
               (keyword "3rd")
               (keyword "4th")))

(-> (pvs) first time)
(-> (pvs) last time)

; 11.3.2 pmap
; like a parallel `map`

(->> [1 2 3]
     (pmap (comp inc (partial sleeper 2)))
     doall
     time)

(->> [1 2 3]
     (map (comp inc (partial sleeper 2)))
     doall
     time)

; 11.3.3 pcalls
(-> (pcalls
      #(sleeper 2 :first)
      #(sleeper 3 :second)
      #(keyword "3rd"))
    doall
    time)

; 11.4 a brief introduction to reducer/fold
(def big-vec (vec (range (* 1000 1000))))
(time (reduce + big-vec))
(time (r/fold + big-vec)) ; way faster! r/fold supports parallelizing work
; in this case, since addition is associative, order doesn't matter, so
; addition can be parallelized (i.e. the accumulated value in the reducer
; doesn't necessarily need to happen in any particular order)
