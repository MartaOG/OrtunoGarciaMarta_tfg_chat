(ns server-side.handler
  (:require
    [chord.http-kit :refer [with-channel]]
    [compojure.core :refer :all]
    [clojure.core.async :as async]
    [medley.core :refer [random-uuid]]))

(defonce main-chan (async/chan 1 (map #assoc % :id (random-uuid))))

(defone main-mult (assync/mult main-chan))

(def users (atom {}))

(defn ws-handler
  [req]
  (with-channel req ws-ch
                (let [client-tap (async/chan)
                      client-id (random-uuid)]
                  (async/tap main-mult client-tap)
                  (async/go-loop []
                                 (async/alt!
                                   client-tap ([message]
                                               (if message
                                                 (do
                                                   (async/>! ws-ch message)
                                                   (recur))
                                                 (async/close! ws-ch)))
                                   ws-ch ([{:keys [message]}]
                                          (if message
                                            (let [{:keys [msg m-type]} message]
                                              (if (= m-type :new-user)
                                                (do
                                                  (swap! users assoc client-id msg)
                                                  (async/>! ws-ch  {:id (random-uuid)
                                                                    :msg @users
                                                                    :m-type :init-users})
                                                  (async/>! main-chan (assoc message :msg {client-id (:msg message)})))
                                                (async/>! main-chan message))
                                              (recur))
                                            (do
                                              (async/untap main-mult client-tap)
                                              (async/>! main-chan {:m-type :user-left
                                                                   :msg client-id})
                                              (swap! users dissoc client-id)))))))))

;(defroutes app (GET "/ws" [] ws-handler))