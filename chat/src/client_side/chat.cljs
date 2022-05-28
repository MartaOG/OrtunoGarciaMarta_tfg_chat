(ns ^:figwheel-hooks client-side.chat
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [chord.client :refer [ws-ch]]
   [cljs.core.async :as async :include-macros true]))

(def info {:text "Client side"})

;;To capt message we save the text here.
;(def msg {:message ""})

;;To save all message between dif users.
(def chat-history {:conversation []})

(defonce app-state (atom info))
(defonce send-chan (async/chan))
(defonce chat-history (atom []))
(def user "Marta")                                          ; to do trials


;;USER INTERACTION
(defn button-to-send [msg]
  [:div {:class "btn-client-sender"}
   [:button {:type "submit"
             :on-click msg} "Send"]])

(defn send-message [msg]
  (async/put! send-chan msg))

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


;;WEBSOCKETS
(defn see-chat []
  (reagent/create-class
    {:render (fn []
               [:div {:class "history"}
                (for [one-msg @chat-history]
                  ^{:key (:id one-msg)} [:p (str (:user one-msg) ": " (:msg one-msg))])])
     :component-did-update (fn [this]
                             (let [node (reagent/dom-node this)]
                               (set! (.-scrollTop node) (.-scrollHeight node))))}))

(defn send-chat
  [server]
  (async/go-loop []
                 (when-let [msg (async/<! send-chan)]
                   (async/>! server msg)
                   (recur))))

(defn show-chat []
  (reagent/create-class
    {:render (fn []
               [:div (for [message @chat-history]
                       ^{:key (:id message)}
                       [:p (str (:msg message))])])}))

(defn send-message [msg]
  (async/put! send-chan msg))

(defn receive-msgs
  [svr-chan]
  (async/go-loop []
                 (if-let [new-msg (:message (<! svr-chan))]
                   (do
                     (case (:m-type new-msg)
                       :chat (swap! chat-history conj (dissoc new-msg :m-type)))
                     (recur))
                   (println "Websocket closed"))))

(defn setup-websockets! []
  (async/go
    (let [{:keys [ws-channel error]} (async/<! (ws-ch ws-url))]
      (if error
        (println "Something went wrong with the websocket")
        (do
          (send-message {:msg (@app-state)})
          (send-chat ws-channel)
          (receive-msgs ws-channel))))))

;;MAIN
(defn chat-view []
  [:div {:class "chat-container"}
   [see-chat]
   [write-msg]
   [:div
    [:h3 "core.async chat room"]]
   ])

(defn get-app-element []
  (gdom/getElement "app"))

(defn main []
  (setup-websockets!)
  [:div
   [:h1 (:text @app-state)]
   [see-chat]
   [write-msg []]])

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
