(ns dondai.storage
  (:require
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [react-native-sqlite-storage :as s]
   [dondai.logging :refer [get-logger]]))

(s/enablePromise true)

(def log (get-logger "dondai.storage"))

(def logsuccess (partial log "success"))
(def logerror (partial log "error"))

(defonce sqlitedb nil)

;; SQLite.openDatabase({name: 'my.db', location: 'Shared'}, successcb, errorcb);
(defn in-transaction
  ([txfn & {:keys [on-success on-error]
            :or {on-success #(log "Transaction Success")
                 on-error   #(log "Transaction error" %) }} ]
   (.transaction sqlitedb
                 txfn
                 on-error
                 on-success)))

(defn res-item [res i]
  (js->clj (.item (.-rows res) i)
           :keywordize-keys true))

(defn res-length [res i]
  (.-length (.-rows res)))

(defn res->clj [res]
  {:rows (map (fn [i] (res-item res i))
              (range (res-length res)))
   :length (res-length res)
   :rows-affected (.-rowsAffected res)
   :insert-id (.-insertId res)})

(defn sql
  ([transaction query & {:keys [params on-success on-error]
                         :or {params     []
                              on-success #(log (str "Query " query " => %2"))
                              on-error   #(log (str "Error executing query: " query "=>" %2))}}]
   (.executeSql transaction
                query
                (clj->js params)
                (fn [tx res] (on-success tx (res->clj res)))
                on-error)))

(defn add-taskx [& args]
  (log "boooo")

  (in-transaction
   (fn [tx]
     (sql tx "CREATE TABLE IF NOT EXISTS test (examplecol TEXT)"
          :on-success (partial logsuccess "create")
          :on-error (partial logerror "create"))
     (sql tx "INSERT INTO test (examplecol) VALUES ('test')"
          :on-success (partial logsuccess "insert")
          :on-error (partial logerror "insert"))
     (sql tx "SELECT * FROM test"
          :on-success (partial logsuccess "select")
          :on-error (partial logerror "select"))
     )
   :on-error (fn [& args]
               (log "Error executing transaction" args))
   ))


(def create-tasks-table-query
  "CREATE TABLE IF NOT EXISTS tasks (
                     id INTEGER PRIMARY KEY,
                     title TEXT NOT NULL,
                     description TEXT,
                     allotted_time INTEGER
                   )")

(defn initdb [on-success on-error]
  (go
    (set! sqlitedb (<p! (s/openDatabase "app.db" "1.0" "Dondai" -1
                                        (fn [& args]
                                          (log "Open database successful" args))
                                        (fn [& args]
                                          (log "Open database failed" args)))))
    (<p! (.transaction
          sqlitedb
          (fn [tx]
            (.executeSql tx create-tasks-table-query []
                         #(log "task table query created successfully")
                         on-error))
          log
          on-success))))


(defn add-task [tx title description allotted-time & {:keys [on-success on-error]}]
  (log "inserting task" [title description allotted-time])
  (sql tx "INSERT INTO tasks (title, description, allotted_time) VALUES (?1, ?2, ?3)"
       :params [title description allotted-time]
       :on-success on-success
       :on-error on-error))

(defn fetch-tasks [tx & {:keys [on-success on-error]}]
  (sql tx "SELECT * FROM tasks"
       :on-success on-success
       :on-error on-error))

;; This seems to be the way to get the results of a query:
;; (.transaction sqlitedb
;;               (fn [tx] (.executeSql tx "SELECT * FROM test" []
;;                                     log
;;                                     (fn [& args] (apply log "z" args)))) ;; This
;;               #(log "x" %)
;;               #(log "y" % %1 %2))
