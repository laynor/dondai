(ns dondai.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [react-native-paper :as rnp]
            [reagent.react-native :as rn]
            [dondai.subs :as subs]
            [dondai.events :as events]
            [dondai.i18n :as i18n]
            ;; [react-native-sqlite-storage :as sqlite]
            [dondai.storage :as sto]
            ))

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
   :task-card    {:padding 10}
   :dialog-card    {:padding 32}
   :centered-dialog {:padding 10
                     :justify-content :center
                     :background-color "#33333333"
                     ;; :align-items :center
                     :flex-direction :column
                     :flex 1
                     }
   :barrier {:background-color "#33333333" :width "100%" :height "100%"}
   })

(def log (.-log js/console))

(defn render-tasks [task]
  (r/as-element
   [rn/view {:style (:task-card styles)}
    [:> rnp/Card
     [:> rnp/Card.Title  {:style (:task-card styles)
                          :title (.. task -item -title)}]
     [:> rnp/Card.Content
      [:> rnp/Paragraph (.. task -item -description)]]]]))

(defn modal [visibile-if hide-action & children]
  [rn/modal {:animation-type   "fade"
             :background-color "#44444444"
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

(defn app []
  [rn/view {:style (:container styles)}
   [:> rnp/Appbar.Header {:style {:width "100%"}} ;; {:style (:bottom styles)}
    [:> rnp/Appbar.Content {:title (i18n/tr :app-title)}]
    [:> rnp/Appbar.Action {:icon    "plus"
                           :onPress #(rf/dispatch [:show-create-dialog])}]]
   [rn/flat-list
    {:data          @(rf/subscribe [:get-tasks])
     :key-extractor (fn [task _] (str (.. task -id)))
     :renderItem    render-tasks}]
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

    [:> rnp/Button {:icon "content-save"
                    :mode "contained"
                    :onPress #(let [title @(rf/subscribe [:new-task-title])
                                    descr @(rf/subscribe [:new-task-description])]
                                (prn title descr)
                                (sto/in-transaction
                                 (fn [tx] (sto/add-task tx title descr 60
                                                        :on-success (fn [tx _res]
                                                                      (sto/fetch-tasks
                                                                       tx
                                                                       :on-success (fn [_ res]
                                                                                     (log (js->clj res))
                                                                                     ;; (dispatch [:set-tasks])
                                                                                     ))
                                                                      )
                                                        )))
                                (log ">>>>>>>>>>>>>>." title descr))}
     (i18n/tr :new-task-dialog/save)]
    ]
   ])


(defn root []
  [:> rnp/Provider
   [app]])

(defn hello []
  [rn/view {:style {:flex 1 :align-items "center" :justify-content "center"}}
   [rn/text {:style {:font-size 50}} "Hello Mist Krell!"]])

;; (defn app []
;;   [:> rnp/Provider
;;    [:> rnp/Card
;;     [:> rnp/Card.Title {:style {:task-card styles}
;;                         :title "mantani"}]]])

(defn ^:export -main [& args]
  (sto/initdb #(log "init DB failed"))
  (rf/dispatch-sync [:initialize-db])
  (r/as-element [root]))
