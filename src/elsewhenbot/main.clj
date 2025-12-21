(ns elsewhenbot.main
  (:gen-class)
  (:require [tg-clj.core :as tg]
            [tg-clj-server.defaults :as defaults]
            [tg-clj-server.poll :as tg-poll]
            [tg-clj-server.utils :as tu]
            [clojure.tools.logging :as log]

            [elsewhenbot.handlers.convert :as convert]
            [elsewhenbot.handlers.commands :as commands])
  (:import (java.time Instant Duration)))

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

;; Health check mechanism
(def ^:private last-heartbeat (atom (Instant/now)))

(defn- heartbeat-middleware
  "Middleware that updates the last heartbeat timestamp on each update poll"
  [handler]
  (fn [req]
    (reset! last-heartbeat (Instant/now))
    (handler req)))

(defn- get-health-check-threshold-seconds
  "Get the health check threshold in seconds from environment variable.
   Defaults to 120 seconds (2 minutes) if not set."
  []
  (if-let [env-val (System/getenv "HEALTH_CHECK_THRESHOLD_SECONDS")]
    (try
      (Long/parseLong env-val)
      (catch NumberFormatException _
        (log/warn "Invalid HEALTH_CHECK_THRESHOLD_SECONDS value, using default of 120")
        120))
    120))

(defn- start-watchdog
  "Starts a watchdog thread that monitors the last heartbeat.
   If no heartbeat is received within the threshold, the process exits with status 1."
  [threshold-seconds]
  (let [check-interval-ms (* 1000 (max 10 (quot threshold-seconds 4)))
        running? (atom true)]
    (log/info (format "Starting health check watchdog with threshold of %d seconds" threshold-seconds))
    (future
      (while @running?
        (try
          (Thread/sleep check-interval-ms)
          (let [now (Instant/now)
                last-beat @last-heartbeat
                elapsed-seconds (.getSeconds (Duration/between last-beat now))]
            (if (> elapsed-seconds threshold-seconds)
              (do
                (log/error (format "Health check failed! No updates received in %d seconds (threshold: %d). Exiting..."
                                   elapsed-seconds threshold-seconds))
                (System/exit 1))
              (log/debug (format "Health check OK - last heartbeat %d seconds ago" elapsed-seconds))))
          (catch InterruptedException _
            (reset! running? false))
          (catch Exception e
            (log/error e "Error in watchdog thread"))))
      (log/info "Watchdog stopped"))
    #(reset! running? false)))

(defn start []
  (let [bot (tg/make-client
              {:token @token
               :timeout 35000})
        path (or (System/getenv "DATA_PATH")
                 "/data/data.edn")
        ;; Wrap app with heartbeat middleware
        base-app (defaults/make-app routes {:store/path path})
        app (heartbeat-middleware base-app)
        ;; Start the watchdog
        threshold-seconds (get-health-check-threshold-seconds)
        stop-watchdog (start-watchdog threshold-seconds)
        ;; Start the polling server
        stop-handle (future (tg-poll/run-server bot app))]
    (log/info "Started bot!")
    #(do (log/info "Stopped bot!")
         (stop-watchdog)
         (future-cancel stop-handle))))

(comment
  (def stop (start))
  (stop))

(defn -main [& _args]
  (start)
  @(promise))
