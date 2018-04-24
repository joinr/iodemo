(ns iodemo.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async]
            [iodemo.io :as io]            
            [reagent.core :as r]))            

(enable-console-print!)

(println "This text is printed from src/iodemo/core.cljs. Go ahead and edit it and see reloading in action.")
(println "heyo!!!")

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (r/atom {:text "Hello world!"}))
                                    
;;optionally touch your app-state to force rerendering depending on
;;your application
(defn on-js-reload [])

;;notice, these are reactions, we're wiring stuff up manually.
;;in other frameworks, like re-frame, we can define this a bit
;;more declaratively. 
(defn load-file! [& {:keys [source]}]
  (go (let [txt (->> (io/current-file :el source) 
                     (io/file->lines!!)
                     (async/<!)
                     (clojure.string/split-lines))]      
        (swap! app-state assoc :text txt)
        nil))
  nil)

(defn file-selector []  
   [:div {:id "file-selector"}
       "Select a file, then load it!"
       [:form 
        ;;file selector dialogue
        "Input-file:"   [:input {:type     "file"
                                 :name     "infile"
                                 :id       "infile"
                                 :on-click (fn [e] (println "selecting-file!"))}]
        ;;actually execute the load
        "Load-File:"   [:input {:type "button"
                                :name "load-file-button"
                                :id   "load-file-button"
                                :on-click (fn [e]
                                            (do (load-file! :source "infile"))
                                            (println "loading-file!!"))}]]])

(defn app-body []
  (let []
    (fn [] 
      (let [{:keys [file-path text]} @app-state]
        [:div {}      
         [:h2 "Welcome to a dumb IO example!"]
         [:p "Once you select a file, interactive widgets will render the file content!"]
         [file-selector]
         [:h2 "File Lines:"]
         [:div {:id "File Contents"}
          [:p (or (when (seq text)
                    (for [ln text]
                      [:p ln]))
                  "file contents go here.....")]]]))))

;;initialize the reactive renderer, targeting the "reagent-app"
;;element in index.html
(r/render [app-body] 
          (.getElementById js/document "reagent-app"))
    


