(ns bot.core
  (:require [clojure.core.async :refer [<!!]]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [morse.handlers :as h]
            [morse.polling :as p]
            [morse.api :as t]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clj-time.local :as l])
  (:gen-class))

(def base-url "https://api.telegram.org/bot")

(def nome-linha
  {
    1 "Azul" 
    2 "Verde"
    3 "Vermelha"
    4 "Amarela"
    5 "Lilás"
    7 "Rubi"
    8 "Diamante"
    9 "Esmeralda"
    10 "Turquesa"
    11 "Coral"
    12 "Safira"
    15 "Prata"})
    
(def formated-subway-linas-name
  (str 
    "1: " (nome-linha 1)
    "\n2: " (nome-linha 2)
    "\n3: " (nome-linha 3)
    "\n4: " (nome-linha 4)
    "\n5: " (nome-linha 5)
    "\n7: " (nome-linha 7)
    "\n8: " (nome-linha 8)
    "\n9: " (nome-linha 9)
    "\n10: " (nome-linha 10)
    "\n11: " (nome-linha 11)
    "\n12: " (nome-linha 12)
    "\n15: " (nome-linha 15)))

  ; TODO: fill correct token
(def token (env :telegram-token))

(def date (java.util.Date.))
(def df (java.text.SimpleDateFormat. "EEE MMM d HH:mm:ss zzz yyyy"))

(defn parse-date [_date]
  (let [date (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") _date)]
    (.format (java.text.SimpleDateFormat. "dd/MM/yyyy HH:mm") date)))

; Status Metro
(defn lista []
  (json/read-json ((client/get "https://www.diretodostrens.com.br/api/status") :body)))

(defn format-message [message]
  (if (first message)
    (let [m (first message)]
      (str 
        "Situação: " (m :situacao) 
        "\nLinha: " (nome-linha (m :codigo)) 
        "\nHorário: " (parse-date (m :modificado))))
    (str "Linha não encontrada")))

(defn status [id]
  (try
    (let [filtro (Integer/parseInt (clojure.string/replace id #"/" ""))]
      (format-message (filter #(= filtro (:codigo %)) (lista))))
    (catch Exception e (str "Command not found " (.getMessage e)))))

(h/defhandler handler

  (h/command-fn "help"
    (fn [{{id :id :as chat} :chat}]
      (println "Help was requested in " chat)
      (t/send-text token id "/linhas-metro mostra todas as linhas \n/numero mostra o status da linha ex: /1")))

  (h/command-fn "start"
    (fn [{{id :id :as chat} :chat}]
      (println "Help was requested in " chat)
      (t/send-text token id "/linhas-metro mostra todas as linhas \n/numero mostra o status da linha ex: /1")))       
  
  (h/command-fn "linhas-metro"
    (fn [{{id :id :as chat} :chat}]
      (println "Get all subway lines. " chat)
      (t/send-text token id formated-subway-linas-name)))
  
  (h/message-fn
    (fn [{{id :id} :chat :as message}]
      (println "Intercepted message: " message)
      (if message
        (t/send-text token id (status (message :text)))
        (println "Intercepted message: " message)))))
         
(defn -main
  [& args]
  (when (str/blank? token)
    (println "Please provde token in TELEGRAM_TOKEN environment variable!")
    (System/exit 1))

  (println "Starting the bot")    
  (<!! (p/start token handler)))
