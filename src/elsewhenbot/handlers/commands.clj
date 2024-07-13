(ns elsewhenbot.handlers.commands
  (:refer-clojure :exclude [remove])
  (:require [tg-clj-server.utils :as tu]
            [elsewhenbot.utils :as u]
            [clojure.string :as str]))

(defn message-text [req]
  (get-in req [:update :message :text]))

(defn command-args [text]
  (let [args (str/trim (second (re-find #".*?/[^ ]+(.*)" text)))]
    (when (seq args)
      args)))

(defn add
  "Add a timezone to output to the chat"
  [{u :update :keys [store] :as req}]
  (let [maybe-tz (command-args (message-text req))]
    (if (u/parse-tz maybe-tz)
      (let [chat-id (get-in u [:message :chat :id])
            new-store (update-in store [:chat chat-id :tzs] (fnil conj #{}) maybe-tz)]
        (-> (u/react-with "👌" u)
            (assoc :set-store new-store)))
      (-> {:op :sendMessage
           :request {:text "Invalid timezone"}}
          (tu/reply-to u)))))

(defn remove
  "Remove a timezone from output to the chat"
  [{u :update :keys [store] :as req}]
  (let [maybe-tz (command-args (message-text req))]
    (if (u/parse-tz maybe-tz)
      (let [chat-id (get-in u [:message :chat :id])
            new-store (update-in store [:chat chat-id :tzs] disj maybe-tz)]
        (-> (u/react-with "👌" u)
            (assoc :set-store new-store)))
      (-> {:op :sendMessage
           :request {:text "Invalid timezone"}}
          (tu/reply-to u)))))

(defn me
  "Set your own timezone"
  [{u :update :keys [store] :as req}]
  (let [maybe-tz (command-args (message-text req))]
    (if (u/parse-tz maybe-tz)
      (let [user-id (get-in u [:message :from :id])
            new-store (assoc-in store [:user user-id :tz] maybe-tz)]
        (-> (u/react-with "👌" u)
            (assoc :set-store new-store)))
      (-> {:op :sendMessage
           :request {:text "Invalid timezone"}}
          (tu/reply-to u)))))

(defn default
  "Set chat default timezone (Defaults to UTC otherwise)"
  [{u :update :keys [store] :as req}]
  (let [maybe-tz (command-args (message-text req))]
    (if (u/parse-tz maybe-tz)
      (let [chat-id (get-in u [:message :chat :id])
            new-store (assoc-in store [:chat chat-id :default-tz] maybe-tz)]
        (-> (u/react-with "👌" u)
            (assoc :set-store new-store)))
      (-> {:op :sendMessage
           :request {:text "Invalid timezone"}}
          (tu/reply-to u)))))
