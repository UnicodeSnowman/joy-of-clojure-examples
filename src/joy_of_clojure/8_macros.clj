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
