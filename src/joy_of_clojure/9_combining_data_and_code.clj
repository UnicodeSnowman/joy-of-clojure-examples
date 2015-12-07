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
