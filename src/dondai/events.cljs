(ns dondai.events
  (:require
   [re-frame.core :refer [reg-event-db after]]
   [clojure.spec.alpha :as s]
   [dondai.db :as db :refer [app-db]]))

;; -- Interceptors ------------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md
;;

(def log (.-log js/console))

(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db [event]]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check after " event " failed: " explain-data) explain-data)))))

(def validate-spec
  (if goog.DEBUG
    (after (partial check-and-throw ::db/app-db))
    []))

;; -- Handlers --------------------------------------------------------------

(reg-event-db
 :initialize-db
 validate-spec
 (fn [_ _]
   app-db))

(reg-event-db
 :inc-counter
 validate-spec
 (fn [db [_ _]]
   (update db :counter inc)))

(reg-event-db
 :add-task
 validate-spec
 (fn [db [_ _]]
   (update db :tasks
           (fn [tasks] (conj tasks {:title "Bizz" :description "descr" :allotted-time 60})))))

(reg-event-db
 :show-create-dialog
 validate-spec
 (fn [db [_ _]]
   (assoc db :create-task-dialog-visible? true)))

(reg-event-db
 :hide-create-dialog
 validate-spec
 (fn [db [_ _]]
   (assoc db :create-task-dialog-visible? false)))

(reg-event-db
 :set-new-task-title
 validate-spec
 (fn [db [_ value]]
   (assoc db :new-task-title value)))

(reg-event-db
 :set-new-task-description
 validate-spec
 (fn [db [_ value]]
   (assoc db :new-task-description value)))

(reg-event-db
 :save-new-task
 validate-spec
 (fn [db [_ title description allotted-time]]
   ;; (storage/add-task title description allotted-time)
   ;; (storage/in-transaction
   ;;  (fn [tx]
   ;;    (storage/add-task tx title description allotted-time
   ;;                      ;; :on-success (fn [tx res]
   ;;                      ;;               (storage/fetch-tasks tx))
   ;;                      )))
   (-> db
       (update :tasks #(conj % {:title title :description description :allotted-time allotted-time}))
       (assoc :new-task-description ""
              :new-task-title ""
              :create-task-dialog-visible? false))
   ))
