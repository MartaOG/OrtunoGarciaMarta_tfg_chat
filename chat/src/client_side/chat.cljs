(ns ^:figwheel-hooks client-side.chat
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]))

(defn multiply [a b] (* a b))

(def info {:text "Client side"})

;;To capt message we save the text here.
(def msg {:message ""})

;;To save all message between dif users.
(def chat-history {:conversation []})

(defonce app-state (atom info))
(defonce message (atom msg))

(defn get-app-element []
  (gdom/getElement "app"))

(defn main []
  [:div
   [:h1 (:text @app-state)]
   [write-message []]
   [button-to-send []]])

(defn write-message []
  [:div {:class "input-client-message"}
   [:h3 "Say 'Hi!'"]
   [:input {:type :text
            :placeholder "Write here your message"
            :value (:message @message)
            :on-change #(swap! message assoc :message (-> % .-target .-value))}]
   ])

(defn button-to-send []
  [:div {:class "btn-client-sender"}
   [:button {:on-click send-message} "Send"]])

(defn send-message []
  (println @message))

(defn mount [el]
  (rdom/render [main] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
