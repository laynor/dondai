(ns dondai.db
  (:require [clojure.spec.alpha :as s]))

;; spec of app-db
(s/def ::tasks vector?)
(s/def ::counter number?)
(s/def ::create-task-dialog-visible? boolean?)
(s/def ::new-task-title string?)
(s/def ::new-task-allotted-time int?)
(s/def ::app-db
  (s/keys :req-un [::counter ::tasks ::create-task-dialog-visible? ::new-task-title]))


;; initial state of app-db
(defonce app-db {:counter 0
                 :create-task-dialog-visible? false
                 :tasks []
                 :new-task-title ""
                 :new-task-description ""
                 :new-task-allotted-time 30})
