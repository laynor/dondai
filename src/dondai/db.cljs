(ns dondai.db
  (:require [clojure.spec.alpha :as s]))

;; spec of app-db
(s/def ::tasks vector?)
(s/def ::counter number?)
(s/def ::create-task-dialog-visible? boolean?)
(s/def ::new-task-title string?)
(s/def ::app-db
  (s/keys :req-un [::counter ::tasks ::create-task-dialog-visible? ::new-task-title]))


;; initial state of app-db
(defonce app-db {:counter 0
                 :create-task-dialog-visible? false
                 :tasks [{:id 0
                          :title "Play some guitar"
                          :allotted-time 30
                          :description "Play every day, you'll feel and get better!"}
                         {:id 1
                          :title "Study Japanese"
                          :allotted-time 30
                          :description "You can get better just practicing some time every day :)"}

                         {:id 2
                          :title "Study Japanese"
                          :allotted-time 30
                          :description "You can get better just practicing some time every day :)"}]
                 :new-task-title ""
                 :new-task-description ""})
