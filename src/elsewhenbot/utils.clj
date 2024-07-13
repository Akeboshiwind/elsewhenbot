(ns elsewhenbot.utils
  (:import (org.joda.time DateTimeZone)))

(defn str->tzid [s]
  (DateTimeZone/forID s))

(defn parse-tz [s]
  (when s
    (try
      (str->tzid s)
      (catch Exception _
        nil))))

(defn react-with [emoji u]
  (let [emoji (if (string? emoji)
                [emoji]
                emoji)
        message (:message u)
        message-id (:message_id message)
        chat-id (get-in message [:chat :id])]
    {:op :setMessageReaction
     :request {:chat_id chat-id
               :message_id message-id
               :reaction (map (fn [e] {:type "emoji" :emoji e}) emoji)}}))
