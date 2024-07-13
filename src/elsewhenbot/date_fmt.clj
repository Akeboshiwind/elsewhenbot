(ns elsewhenbot.date-fmt
  (:import (org.joda.time.format DateTimeFormat DateTimeFormatter)))

(def human-fmt (DateTimeFormat/forPattern "EEEE, MMMM d, yyyy HH:mm:ss a"))

(defn dt->human [dt]
  (.toString dt human-fmt))

(comment
  (dt->human (org.joda.time.DateTime.))
  (dt->human (org.joda.time.DateTime.)))
