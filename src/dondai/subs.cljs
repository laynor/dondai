(ns dondai.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :get-counter
 (fn [db _]
   (:counter db)))

(reg-sub
 :get-tasks
 (fn [db _]
   (:tasks db)))

(reg-sub
 :create-task-dialog-visible?
 (fn [db _]
   (:create-task-dialog-visible? db)
   ))

(reg-sub
 :new-task-title
 (fn [db _]
   (:new-task-title db)))

(reg-sub
 :new-task-description
 (fn [db _]
   (:new-task-description db)))

(reg-sub
 :new-task-allotted-time
 (fn [db _]
   (:new-task-allotted-time db)))
