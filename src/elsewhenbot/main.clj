(ns elsewhenbot.main
  (:gen-class)
  (:require [tg-clj.core :as tg]
            [tg-clj-server.poll :as poll]
            [tg-clj-server.webhook :as webhook]
            [tg-clj-server.defaults.poll :as poll-defaults]
            [tg-clj-server.defaults.webhook :as webhook-defaults]
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

(defn start []
  (let [token (or (System/getenv "BOT_TOKEN")
                  (throw (Exception. "BOT_TOKEN is not set")))
        webhook-url (System/getenv "WEBHOOK_URL")
        webhook-secret (System/getenv "WEBHOOK_SECRET")
        path (or (System/getenv "DATA_PATH")
                 "/data/data.edn")
        client (tg/make-client {:token token :timeout 35000})]
    (if webhook-url
      ;; Webhook mode
      (let [app (webhook-defaults/make-app routes {:store/path path})
            port (parse-long (or (System/getenv "PORT") "8080"))]
        (log/info "Starting webhook mode on port" port)
        (tg/invoke client {:op :setWebhook
                           :request {:url webhook-url
                                     :secret_token webhook-secret}})
        (webhook/run-server client app {:port port
                                        :secret-token webhook-secret}))
      ;; Polling mode
      (let [app (poll-defaults/make-app routes {:store/path path})]
        (log/info "Starting polling mode")
        (tg/invoke client {:op :deleteWebhook})
        (poll/run-server client app)))))

(comment
  ;; Polling mode (default):
  ;; BOT_TOKEN=<token> clj -M:dev -m elsewhenbot.main

  ;; Webhook mode:
  ;; BOT_TOKEN=<token> WEBHOOK_URL=https://... WEBHOOK_SECRET=... PORT=8080 clj -M:dev -m elsewhenbot.main

  (def stop (start))
  (stop))

(defn -main [& _args]
  (start)
  @(promise))
