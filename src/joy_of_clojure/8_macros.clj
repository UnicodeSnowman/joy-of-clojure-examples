(ns joy-of-clojure.8-macros)

(-> 25 Math/sqrt int list)

; 8.1 Data is code is data
; (eval (list 1 2)) ; fail
; (eval (1 2)) ; fail
(eval '(list 1 2)) ; ☃ !
; eval tries to evaluate the thing it is passed... which is why
; (eval (list 1 2)) fails... i.e.  it runs (eval (1 2))

(eval (list (symbol "+") 1 2)) ; => (eval (+ 1 2))
(list (symbol "+") 1 2) ; => (+ 1 2)

; 8.1.1 Syntax-quote, unquote, and splicing
(mapcat (fn [v] (list (* v 2))) [1 2 3 4])

(mapcat
  (fn [[k v]] ; unpack each collection item into key and value
    [k v])
  {:a 1 :b 2}) ; works simply with symbols...

(mapcat
  (fn [[k v]] ; unpack each collection item into key and value
    [k v])
  '{a 1 b 2}) ; otherwise, we quote

(defn contextual-eval [ctx expr]
  (eval
    `(let [~@(mapcat (fn [[k v]] [k `'~v]) ctx)] ; why the fuck do we need `'~v? seems to work fine as just v
       ~expr))) ; apparently `'~ gets the value of the build bindings at runtime?

; NOTE ... [k `v]) ctx)] would work fine if we declared a (def v 123) var
; in parent context!

(contextual-eval '{a 1 b 2} '(+ a b))
(contextual-eval '{a 1 b 2} '(let [b 1000] (+ a b)))

; handling nested syntax quotes
(let [x 9
      y '(- x)]
  (println `y)                      ; joy-of-clojure.8-macros/y
  (println ``y)                     ; (quote joy-of-clojure.8-macros/y)
  (println ``~y)                    ; (joy-of-clojure.8-macros/y)
  (println ``~~y)                   ; (- x)
  (contextual-eval {'x 36} ``~~y))  ; -36

; THE POINT: clojure supports manipulating structures into different
; executable forms at both runtime and compile time via quoting!

; 8.2 Defining control structures

; w/o syntax quote
(nnext [true '(prn 1) false '(prn 2)]) ; => (false (prn 2))

(defmacro do-until [& clauses]
  (when clauses
    (list 'clojure.core/when (first clauses)
          (if (next clauses)
            (second clauses)
            (throw (IllegalArgumentException.
                     "do-until requires an even number of forms")))
          (cons 'do-until (nnext clauses)))))

(do-until
  true (prn 1)
  false (prn 2))
(macroexpand-1 '(do-until true (prn 1) false (prn 2)))

; w/ syntax-quote and unquoting
(defmacro unless [condition & body]
  `(if (not ~condition) ; unquoting here...
     (do ~@body))) ; and here provides "blanks" to be filled

(unless (even? 3) "Now we see it...")
(unless (even? 2) "Now we don't...")
(unless false (println "yep!"))

; 8.3 Macros combining forms
; used examle of `defn` to create functions with name, doc string,
; metadata, etc.

; add-watch
(defmacro def-watched [name & value]
  `(do
     (def ~name ~@value)
     (add-watch (var ~name)
                :re-bind
                (fn [~'key ~'r old# new#]
                  (prn old# " -> " new#)))))

(macroexpand-1 '(def-watched x 2))

(def-watched x (* 12 12))
(prn x)
(def x 0)

; 8.4 using macros to change forms
(declare grok-props grok-attrs)

((comp not vector?) [1 2 3])

(defn handle-things [things]
  (for [t things]
    {:tag :thing
     :attrs (grok-attrs (take-while (comp not vector?) t))
     :content (if-let [c (grok-props (drop-while (comp not vector?) t))]
                [c]
                [])}))

(defmacro domain [name & body]
  `{:tag :grouping
    :attrs {:name (str '~name)}
    :content [~@(handle-things body)]})

(defn grok-attrs [attrs]
  (into {:name (str (first attrs))}
        (for [a (rest attrs)]
          (cond
            (list? a) [:isa (str (second a))]
            (string? a) [:comment a]))))

(defn grop-props [props]
  (when props
    {:tag :properties
     :attrs nil
     :content (apply vector (for [p props]
                              {:tag
                               :property
                               :attrs {:name (str (first p))}
                               :content nil}))}))

(domain man-vs-monster [1 2 3])
