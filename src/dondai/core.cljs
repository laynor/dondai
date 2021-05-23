(ns dondai.core
  (:require [clojure.string :as string]
            [cljs.pprint :refer [pprint]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [react-native-paper :as rnp]
            [reagent.react-native :as rn]
            [dondai.subs :as subs]
            [dondai.events :as events]
            [dondai.i18n :as i18n]
            [cljs.core.async :refer [go chan <! >!] ]
            [cljs.core.async.interop :refer-macros [<p!]]
            [dondai.storage :as sto]
            [dondai.logging :refer [get-logger] :refer-macros [defloggers dbg]]
            [dondai.icons :refer [MaterialIcons MaterialCommunityIcons]]
            ))

(defloggers "dondai.core")

(def colors
  {:base-purple "#3F51B5"
   :app-bar :base-purple
   :play-icon-color :white
   :dim "#33333333"})

(defn get-color [color]
  (condp apply [color]
    nil?     nil
    string?  color
    keyword? (recur (or (get colors color) (name color)))
    (throw (str "invalid argument: " (pr-str color)))))

(defn dbg> [o]
  (log "==> " o)
  o)

(def styles
  {:container   {:flex             1
                 :background-color :white
                 :align-items      :center
                 :justify-content  :flex-start
                 :padding-top      0}
   :counter     {:font-weight   :bold
                 :font-size     24
                 :color         :blue
                 :margin-bottom 20}
   :button      {:font-weight      :bold
                 :font-size        18
                 :padding          6
                 :background-color :blue
                 :border-radius    999
                 :margin-bottom    20}
   :button-text {:padding-left  12
                 :padding-right 12
                 :font-weight   :bold
                 :font-size     18
                 :color         :white}
   :logo        {:width  200
                 :height 200}
   :creds       {:font-weight :normal
                 :font-size   15
                 :color       :blue}
   ;; :bottom       {:position :absolute :left 0 :right 0 :bottom 0 :width "100%"}
   :title        {:color :white :font-size 20}
   :task-card    {:padding 10 :width "100%"}
   :dialog-card    {:padding 32}
   :centered-dialog {:padding 10
                     :justify-content :center
                     :background-color (get-color :dim)
                     :flex-direction :column
                     :flex 1
                     }
   :barrier {:background-color (get-color :dim) :width "100%" :height "100%"}
   })


(def time-circle-radius 30)

(defn format-time [minutes]
  (let [m (mod minutes 60)
        h (/ (- minutes m) 60)]
    (str (if (zero? h) "" (str h "h"))
         (if (and (not (zero? m)) (not (zero? h))) " " "")
         (if (zero? m) "" (str m "m")))))

(defn render-task [task]
  (r/as-element

   [rn/view {:style (:task-card styles)}
    [:> rnp/Card
     [:> rnp/Card.Title  {:style (:task-card styles)
                          :title (:title task)}]
     [:> rnp/Card.Content

      [rn/view {:style {:flexDirection "row" :width "100%"}}
       [rn/view {:style {:flexGrow 1}} [:> rnp/Paragraph (:description task)]]
       [:> rnp/Chip {:style {:font-size 22 :background-color (get-color :base-purple)}
                     :theme {:colors {:enabled "yellow" :disabled "green" :text "yellow" :selected "yellow"}}
                     :icon (fn [& args]
                             (try
                               (r/as-element
                                [:> MaterialIcons {:name "play-arrow" :size 20
                                                   :color (get-color :play-icon-color)}])
                               (catch js/Object e
                                 "play")))
                     :disabled false
                     :selected true
                     :selectedColor "yellow"
                     :color "white"
                     :onPress #(log "pressed")
                     :textStyle {:color "white"}}
        (format-time (:allotted_time task))]]]]]))

(defn modal [visibile-if hide-action & children]
  [rn/modal {:animation-type   "fade"
             :background-color (get-color :dim)
             :transparent      true
             :visible          @(rf/subscribe [:create-task-dialog-visible?])
             :onRequestClose   #(rf/dispatch hide-action)}
   [rn/touchable-without-feedback {:style    (:barrier styles)
                                   :on-press #(rf/dispatch hide-action)} ;
    (into [] (concat [rn/view {:style (:centered-dialog styles)}]
                     children))]])

(defn dialog [title {vquery :visibile-if hide :hide-action} & children]
  [modal vquery hide
   [:> rnp/Card
    [:> rnp/Card.Title  {:style (:dialog-card styles)
                         :title title}]
    (into [] (concat [:> rnp/Card.Content] children))]])

(defn dispatch [arg]
  ;; Flush here is needed to avoid "flickering"
  ;; It seems the state is updated too late and
  ;; react native gets the old value after changing
  ;; its internal text input state, so it renders
  ;; the old db state for a short lapse before the
  ;; set-new-task-title action is actually executed
  ;; This might be the same issue faced in the dialog
  ;; before switching to the pure react-native implementation
  ;; XXX it might be worthwhile to make a `rf/dispatch` wrapper
  ;;     that calls r/flush right after dispatch.
  (rf/dispatch-sync arg)
  (r/flush))

(defn refresh-tasks [tx]
  (sto/fetch-tasks
   tx
   :on-success (fn [_ res]
                 (dispatch [:set-tasks (into [] (:rows res))]))))

(defn add-task []
  (let [title @(rf/subscribe [:new-task-title])
        descr @(rf/subscribe [:new-task-description])
        allotted-time @(rf/subscribe [:new-task-allotted-time])]
    (logi "Adding task " title descr)
    (assert (int? allotted-time))
    (sto/in-transaction
     (fn [tx]
       (let [ch (chan)])
       (go (sto/add-task tx title descr allotted-time
                         :on-success (fn [tx _res]
                                       (refresh-tasks tx)
                                       (dispatch [:hide-create-dialog])
                                       )
                         :on-error #(loge "add task error" %&)))))))

(defn app []
  [rn/view {:style (:container styles)}
   [:> rnp/Appbar.Header {:style {:width "100%" :background-color (get-color :base-purple)}}
    [:> rnp/Appbar.Content {:title (i18n/tr :app-title)}]
    [:> rnp/Appbar.Action {:icon    "plus"
                           :onPress #(rf/dispatch [:show-create-dialog])}]]
   [rn/view {:style {:width "100%" :height "100%" :padding-bottom 50}}
    [rn/flat-list
     {:data          @(rf/subscribe [:get-tasks])
      :key-extractor #(.-id %)
      :renderItem    (comp render-task
                           :item
                           #(js->clj % :keywordize-keys true))}]]
   [dialog (i18n/tr :new-task-dialog/title)
    {:visibile-if [:create-task-dialog-visible?]
     :hide-action [:hide-create-dialog]}
    [:> rnp/TextInput {:label (i18n/tr :new-task-dialog/task-title)
                       :value @(rf/subscribe [:new-task-title])
                       :onChangeText #(dispatch [:set-new-task-title %])
                       }]
    [:> rnp/TextInput {:label (i18n/tr :new-task-dialog/description)
                       :value @(rf/subscribe [:new-task-description])
                       :onChangeText #(dispatch [:set-new-task-description %])}]

    [:> rnp/TextInput {:label (i18n/tr :new-task-dialog/allotted-time)
                       :value (pr-str @(rf/subscribe [:new-task-allotted-time]))
                       :onChangeText #(dispatch [:set-new-task-allotted-time (js/parseInt %)])}]

    [:> rnp/Button {:icon "content-save"
                    :mode "contained"
                    :onPress add-task}
     (i18n/tr :new-task-dialog/save)]
    ]])


(defn root []
  [:> rnp/Provider
   [app]])

(defn hello []
  [rn/view {:style {:flex 1 :align-items "center" :justify-content "center"}}
   [rn/text {:style {:font-size 50}} "Hello Mist Krell!"]])

(defn rows [res]
  (into [] (:rows res)))

(defn initialize-state-from-db []
  (sto/initdb
   (fn [& args]
     (logi "Initialize database: success!")
     (sto/in-transaction refresh-tasks))
   #(log "init DB failed")))

(defn ^:export -main [& args]
  (rf/dispatch-sync [:initialize-db])
  (initialize-state-from-db)
  (r/as-element [root]))
