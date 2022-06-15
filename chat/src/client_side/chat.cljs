(ns ^:figwheel-hooks client-side.chat
  (:require
    [goog.dom :as gdom]
    [reagent.core :as rcore :refer [atom]]
    [reagent.dom :as rdom]
    [chord.client :as cclient]
    [cljs.core.async :as async :include-macros true]))

(def info {:text "Client side"})

(defonce app-state (atom info))
(defonce send-chan (async/chan))

;Variable atomica per anar guardant els missatges
(defonce chat-history (atom []))

; Definició de la lògica del botó. En aquest cas es crea amb sintaxi
; HTML i quan el fa el click (:on-click) s'envia el missatge passatç
; per paràmetre [msg]
(defn button-to-send [msg]
  [:div
   [:button {:type "submit"
             :on-click msg} "Send"]])

; Funció que permet veure la pantalla del xat amb els missatges.
(defn see-chat []
  (rcore/create-class
    {:render (fn [](for [m @chat-history]
                 [:p (str (:user m) ": " (:msg m))]))
     :component-did-update
          (fn [this]
            (let [node (rdom/dom-node this)]
            (set! (.-scrollTop node) (.-scrollHeight node))))}))

; Envia l'estat del chat al servidor
(defn send-chat
  [server]
  (async/go-loop []
                 (when-let [msg (async/<! send-chan)]
                   (async/>! server msg)
                   (recur))))

; Posa el missatge al canal
(defn send-message [msg]
  (println msg)
  (async/put! send-chan msg))

; Funció per permet al l'usuari escriure un missatge. Aquesta funció
; també crida a button-to-send ja que està lligada.
(defn write-msg []
  (let [field (atom nil)]
    (fn []
      [:div {:class "text-input"}
       [:form
        {:on-submit (fn [x]
                      (.preventDefault x)
                      (when-let [msg @field] (send-message msg))
                      (reset! field nil))}
        [:div {:style {:display "flex"
                       :flex-direction "column"}}
         [:input {:type "text"
                  :value @field
                  :placeholder "Write here your message"
                  :on-change #(reset! field (-> % .-target .-value))}]
         (button-to-send [field])]]])))

; Funció per agafar el missatges que el servidor li passa
(defn receive-msgs
  [svr-chan]
  (async/go-loop []
   (if-let [new-msg (:message (<! svr-chan))]
     (do (case (:m-type new-msg)
                 :chat
               (swap! chat-history conj (dissoc new-msg :m-type)))
       (recur))
     (println "Websocket closed"))))

; Posada en marxa dels sockets.
(defn setup-websockets! []
  (async/go
    (let [{:keys [ws-channel error]} (async/<! (cclient/ws-ch "http://localhost:9500/ws"))]
      (if error
        (println "ERROR IN SETUP-WEBSOCKETS!")
        (do
          (send-message {:msg (@app-state)})
          (send-chat ws-channel)
          (receive-msgs ws-channel))))))

(defn get-app-element []
  (gdom/getElement "app"))

(defn main []
  (setup-websockets!)
  [:div
   [:h1 (:text @app-state)]
   [write-msg []]
   [see-chat []]])

(defn mount [el] (rdom/render [main] el))

(defn mount-app-element [] (when-let [el (get-app-element)] (mount el)))

(mount-app-element)

(defn ^:after-load on-reload []
  (mount-app-element))