(ns reagenttest1.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

;; -------------------------
;; Component

;; simple component

(defn simple-component []
  [:div
   [:p "I am a component!"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red "] "text."]])

(defn hello-component [name]
  [:p "Hello, " name "!"])

(defn lister [items]
  [:ul
   (for [item items]
     ^{:key item} [:li "Item " item])])

;; count

(def click-count (atom 0))

(defn counting-component []
  [:div
   "The atom " [:code "click-count"] " has value: "
   @click-count ". "
   [:input {:type     "button" :value "Click me!"
            :on-click #(swap! click-count inc)}]])

;; timer

(def seconds-elapsed (atom 0))
(def seconds-flg (atom false))

(defn timer-component []
  (fn []
    (do
      (when (not @seconds-flg)
        (js/setInterval #(swap! seconds-elapsed inc) 1000)
        (comment js/alert @seconds-flg)
        (reset! seconds-flg true))
      [:div "Seconds Elapsed: " @seconds-elapsed])))

;; input

(def val (atom ""))

(defn atom-input [value]
  [:input {:type      "text"
           :value     @value
           :on-change #(reset! value (-> % .-target .-value))}])

;; bmi component

(def bmi-data (atom {:height 180 :weight 80 :bmi nil}))

(defn calc-bmi []
  (let [{:keys [height weight bmi] :as data} @bmi-data
        h (/ height 100)]
    (if (nil? bmi)
      (assoc data :bmi (/ weight (* h h)))
      (assoc data :weight (* bmi h h)))))

(defn slider [param value min max]
  (let [reset (case param :bmi :weight :bmi)]
    [:input {:type      "range" :value value :min min :max max
             :style     {:width "100%"}
             :on-change #(swap! bmi-data assoc
                                param (-> % .-target .-value)
                                reset nil)}]))

(defn bmi-component []
  (let [{:keys [weight height bmi]} (calc-bmi)
        [color diagnose] (cond
                           (< bmi 18.5) ["orange" "underweight"]
                           (< bmi 25) ["inherit" "normal"]
                           (< bmi 30) ["orange" "overweight"]
                           :else ["red" "obese"])]
    [:div
     [:h3 "BMI calculator"]
     [:div
      "Height: " (int height) "cm"
      [slider :height height 100 220]]
     [:div
      "Weight: " (int weight) "kg"
      [slider :weight weight 30 150]]
     [:div
      "BMI: " (int bmi) " "
      [:span {:style {:color color}} diagnose]
      [slider :bmi bmi 10 50]]]))

;; test component

(def test-val (atom ""))

(defn test-component []
  (let [val (cond
              (= @test-val "") "[空です]"
              :else "[OKです]")]
    (js/console.log "test-val:" @test-val)
    [:div
     [:input {:type      "text"
              :value     @test-val
              :on-change #(reset! test-val (-> % .-target .-value))}]
     [:p "test-valの中身:" val "(" @test-val ")"]]))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Hello Reagent Top"]
   [:div [:a {:href "#/about"} "go to about page"]]
   [:hr]
   [simple-component]
   [:hr]
   [hello-component "World"]
   [:hr]
   [lister (range 3)]
   [:hr]
   [timer-component]
   [:hr]])

(defn about-page []
  [:div [:h2 "Page 2 Reagent"]
   [:div [:a {:href "#/"} "go to the home page"]]
   [:hr]
   [counting-component]
   [:hr]
   [timer-component]
   [:hr]
   [:p "value : " @val]
   [:p "input : " [atom-input val]]
   [:hr]
   [:div [:a {:href "#/component"} "go to component page"]]
   [:hr]])

(defn render-simple []
  [:div [:h2 "Page 3 Reagent"]
   [:p "count value " @click-count]
   [:hr]
   [bmi-component]
   [:hr]
   [:div [:a {:href "#/"} "go to the home page"]]
   [:hr]
   [test-component]
   [:hr]])

(defn my-page []
  [:div
   [:h1 "Reagentのテスト"]
   [test-component]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
                    (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
                    (session/put! :current-page #'about-page))

(secretary/defroute "/component" []
                    (session/put! :current-page #'render-simple))

(secretary/defroute "/my-page" []
                    (session/put! :current-page #'my-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn init! []
  (hook-browser-navigation!)
  (reagent/render-component [current-page] (.getElementById js/document "app")))
