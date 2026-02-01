(ns elsewhenbot.main
  (:gen-class)
  (:require [tg-clj.core :as tg]
            [tg-clj-server.defaults.poll :as poll-defaults]
            [tg-clj-server.poll :as tg-poll]
            [tg-clj-server.utils :as tu]
            [clojure.tools.logging :as log]

            [elsewhenbot.handlers.convert :as convert]
            [elsewhenbot.handlers.commands :as commands]))

(defn mention-me? [{u :update}]
  (let [mentions (tu/all-mentions u)]
    (contains? mentions "elsewhenbot")))

(defn migrate-chat? [req]
  (get-in req [:update :message :migrate_to_chat_id]))

(def routes
  [;; >> Commands
   [#(tu/command? "/add" %) #'commands/add]
   [#(tu/command? "/remove" %) #'commands/remove]
   [#(tu/command? "/me" %) #'commands/me]
   [#(tu/command? "/default" %) #'commands/default]
   ;; >> Convert dates
   [mention-me? #'convert/handler]
   ; Handle migrations of chat IDs
   [migrate-chat?
    (fn [{:keys [store] :as req}]
      (let [from (get-in req [:update :message :migrate_from_chat_id])
            to (get-in req [:update :message :migrate_to_chat_id])]
        {:set-store (update store :chat #(-> %
                                             (assoc to (get % from))
                                             (dissoc from)))}))]])

(def token
  (delay
    (or (System/getenv "TELEGRAM_BOT_TOKEN")
        (throw (Exception. "TELEGRAM_BOT_TOKEN is not set")))))

(defn start []
  (let [bot (tg/make-client
              {:token @token
               :timeout 35000})
        path (or (System/getenv "DATA_PATH")
                 "/data/data.edn")
        app (poll-defaults/make-app routes {:store/path path})
        stop (tg-poll/run-server bot app)]
    (log/info "Started bot!")
    #(do (log/info "Stopped bot!")
         (stop))))

(comment
  (def stop (start))
  (stop))

(defn -main [& _args]
  (start)
  @(promise))
