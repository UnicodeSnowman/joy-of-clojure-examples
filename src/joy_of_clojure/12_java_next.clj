(ns joy-of-clojure.12-java-next
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [com.sun.net.httpserver HttpHandler HttpExchange HttpServer]
           [java.net InetSocketAddress URLDecoder URI]
           [java.io File FilterOutputStream]))

(def OK java.net.HttpURLConnection/HTTP_OK)

(defn respond
  ([exchange body]
   (respond identity exchange body))
  ([around exchange body]
   (.sendResponseHeaders exchange OK 0)
   (with-open [resp (around (.getResponseBody exchange))]
     (.write resp (.getBytes body)))))

(defn new-server [port path handler]
  (doto
    (HttpServer/create (InetSocketAddress. port) 0)
    (.createContext path handler)
    (.setExecutor nil)
    (.start)))

(defn default-handler [txt]
  (proxy [HttpHandler]
    []
    (handle [exchange]
      (respond exchange txt))))

(def server
  (new-server
    8123
    "/joy/hello"
    (default-handler "Hey")))

(.stop server 0)
; this is cumbersome though... we need to start and stop server
; every time we make changes

; instead we bind our server handler to the return value of 
; default-handler, you can update at runtime...
(def p (default-handler
         "There's no problem that can't be solved with another level of indirection"))

(def server (new-server 8123 "/" p))

(update-proxy p
  {"handle" (fn [this exchange]
              (respond exchange (str "this is " this)))})

(def echo-handler
  (fn [_ exchange]
    (let [headers (.getRequestHeaders exchange)]
      (respond exchange (prn-str headers)))))

(update-proxy p {"handle" echo-handler})

(defn html-around [o]
  (proxy [FilterOutputStream]
    [o]
    (write [raw-bytes]
      (proxy-super write
                   (.getBytes (str "<html><body>"
                                   (String. raw-bytes)
                                   "</body></html>"))))))
(defn listing [file]
  (-> file .list sort))

(listing (io/file "."))
(listing (io/file "./README/md"))

(defn html-links [root filenames]
  (string/join
    (for [file filenames]
      (str "<a href='"
           (str root
                (if (= "/" root)
                  ""
                  File/separator)
                file)
           "'>"
           file "</a><br>"))))

(defn details [file]
  (str (.getName file) " is "
       (.length file) " bytes."))
(details (io/file "./README.md"))

(defn uri->file [root uri]
  (->> uri
       str
       URLDecoder/decode
       (str root)
       io/file))
(def f (uri->file "." (URI. "/project.clj")))
(details f)


