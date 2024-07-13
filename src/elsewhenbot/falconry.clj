(ns elsewhenbot.falconry
  (:import (java.util Date)
           (org.joda.time DateTimeZone)
           (com.zoho.hawking HawkingTimeParser)
           (com.zoho.hawking.datetimeparser.configuration HawkingConfiguration)
           (com.zoho.hawking.language.english.model ParserOutput)))

(comment (set! *warn-on-reflection* true))

(defn- ->HawkingConfiguration [m]
  (let [config (HawkingConfiguration.)]
    (doseq [[k v] m]
      (case k
        :timezone (.setTimeZone config v)))
    config))

(defn parse-dates
  ([text]
   (parse-dates text (Date.) {}))
  ([text relative-date-or-config]
   (if (map? relative-date-or-config)
     (parse-dates text (Date.) relative-date-or-config)
     (parse-dates text relative-date-or-config {})))
  ([text relative-date config]
   (let [parser (HawkingTimeParser.)
         h-config (->HawkingConfiguration config)
         dates (.parse parser text relative-date h-config "eng")]
     (->> (.getParserOutputs dates)
          (map (fn [^ParserOutput po]
                 (let [range (.getDateRange po)]
                   {:text (.getText po)
                    :start (.getStart range)
                    :end (.getEnd range)})))))))

(comment
  (parse-dates "Not a date")
  (parse-dates "Today at 10pm")
  ;; What am I looking for?
  ;; I want to input:
  ;;  - Some text: Today at 10pm
  ;;  - A timezone to be relative to: 
  (->> (parse-dates "Today at 10pm"
                    (Date.)
                    {:timezone "Europe/London"}
                    #_{:timezone "Pacific/Auckland"})
       (map (fn [{:keys [start]}]
              (.withZone start (DateTimeZone/forID "UTC"))))
       (clojure.string/join "\n")
       (println)))
