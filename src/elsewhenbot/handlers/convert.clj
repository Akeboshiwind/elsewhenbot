(ns elsewhenbot.handlers.convert
  (:require [tg-clj-server.utils :as tu]
            [clojure.string :as str]
            [elsewhenbot.utils :as u]
            [elsewhenbot.falconry :refer [parse-dates]]
            [elsewhenbot.date-fmt :refer [dt->human]])
  (:import (org.joda.time DateTime)))

(defn fmt-tzs [^DateTime date tzs]
  (->> tzs
       (map (fn [tz]
              (str tz ": " (-> date
                               (.withZone (u/str->tzid tz))
                               dt->human))))
       (str/join "\n")))

(def default-tz "UTC")

(defn chat-id [req]
  (get-in req [:update :message :chat :id]))

(defn chat-tzs [{:keys [store] :as req}]
  (or (get-in store [:chat (chat-id req) :tzs])
      [default-tz]))

(defn message-tz [message {:keys [store] :as req}]
  (let [user-id (get-in message [:from :id])]
    (or (get-in store [:user user-id :tz])
        (get-in store [:chat (chat-id req) :default-tz])
        default-tz)))

(defn ->tzs-message [text message-tz tzs]
  (let [dates (parse-dates text {:timezone message-tz})]
    (when (seq dates)
      (let [date (:start (first dates))]
        (fmt-tzs date tzs)))))

(defn reply-to-message [req]
  (get-in req [:update :message :reply_to_message]))

(defn message [req]
  (get-in req [:update :message]))

(defn handler [{u :update :as req}]
  (let [message (or (reply-to-message req)
                    (message req))
        tzs (chat-tzs req)]
    (when-let [bot-reply
               (->tzs-message (:text message)
                              (message-tz message req)
                              tzs)]
      (-> {:op :sendMessage
           :request {:text bot-reply}}
          (tu/reply-to u)))))
