(ns server_side.main
  (:require [org.httpkit.server :as httpkit]
            [server-side.handler :as handler])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& [port]]
  (httpkit/run-server handler/app {:port (or (Integer. port) 8080)}))
