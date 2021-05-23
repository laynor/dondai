(ns dondai.i18n
  (:require [tongue.core :as tongue]
            ["react-native" :as jsrn]))

(def dicts
  {:en {
        :app-title "Dondai"
        :new-task-dialog {:title "Create new task"
                          :task-title "Title"
                          :description "Description"
                          :allotted-time "Allotted time"
                          :save "Save"}
        }
   })

(def translate (tongue/build-translate dicts))

(defn current-locale []
  (if (= (.-OS jsrn/Platform) "ios")
    (.. jsrn/NativeModules -SettingsManager -settings -AppleLocale)
    (.. jsrn/NativeModules -I18nManager -localeIdentifier)
    ))

(defn selected-locale []
  (symbol (clojure.string/replace (current-locale) #"_" "-")))

(defn tr [what & args]
  (apply translate (selected-locale) what args))

(defn moo []
  "dugist")
