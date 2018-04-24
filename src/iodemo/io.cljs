(ns iodemo.io
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async]
            [goog.dom :as dom]))
            

;;note: clojurescript support for the file api
;;is pretty much non-existent at the moment,
;;thus we role our own stuff....

;;so, we need to use array-seq to coerce array-like
;;returns like FileList into seq-compatible stuff.
(defn input->files [& {:keys [el] :or {el "file"}}]
  (->> (dom/getElement el)
       (.-files)
       (array-seq)))
       
;;assuming there's an input form (file selector) with associated
;;with an id of "file" by default
(defn current-file [& {:keys [el] :or {el "file"}}]
  (first (input->files :el el)))

;;rips the selected filenames from the FileList object, 
;;then extracts the paths from the HTML5 FileList
(defn input->filenames [& {:keys [el] :or {el "file"}}]
  (->> (input->files :el el)
       (map (fn [x] (.-name x)))))

;;Create an HTML5 FileReader, set it's "onload" property to the
;;provided callback function (I hate JS callback crap).
(defn ->file-reader [on-load]
  (let [rdr (js/FileReader.)
        _ (set! (.-onload rdr) on-load)]
        
    rdr))
;;Read file fl using the FileReader rdr, returning
;;a string.
(defn read-file! [rdr fl]
  (. rdr (readAsText fl)))

  
;;this isn't exactly what I wanted...
;;I really prefer having a channel to process.
;;I'm still thinking in terms of synchronous io
;;which is utterly incompatible with js/cljs, so
;;we fake it by doing everything via core/async.
;;the downside is, we lose some of the liveness
;;of the repl, i.e. waiting for results doesn't
;;work.  everything happens indirectly....we have
;;to sit back and watch our application - expressed
;;neatly in core/async channel composition -
;;unfold in response to input.

;;Read a File fl, returning an atom that will
;;eventually be updated with the file contents
;;as a string.
(defn file->lines [fl]
  (let [the-result (atom nil)
        rdr (->file-reader
             (fn [_]
               (this-as this
                  (let [txt (.-result this)]
                       ; _ (log! txt)
                        
                   ; (async/put! res txt)
                    (reset! the-result txt)))))
        _   (read-file! rdr fl)]
        
    the-result))

;;Read a File fl, returning a channel that will
;;eventually receive the contents of the file
;;as a string.
(defn file->lines!! [fl]
  (let [result (async/chan)
        rdr (->file-reader
             (fn [_]
               (this-as this
                  (let [txt (.-result this)]
                       ; _ (log! txt)
                        
                    (go                     
                      (async/put! result txt)
                      (async/close! result))))))
                      
        _  (read-file! rdr fl)]
    result))


;;for pedagocial purposes, these are two
;;ways to synchronously read files.
;;They'd work fine in a serve-side
;;js platform like node or rhino.
;;The problem is, in the browser 
;;security model, you can't 
;;read local files without user
;;initiation.
;;So, that means when you use either
;;method, you get an empty result.
;;Outside the browser, should be
;;fine.
;;So, users must select files
;;using the file API and 
;;input forms no matter what.

#_(defn await! 
   ([the-atom ms]
    (if-let [res @the-atom] 
      res
      (js/setTimeout (fn [] 
                       (await! the-atom)) ms))))

#_(defn read-file [fl]
   (let [f   (js/XMLHttpRequest)
         res (atom nil)
         _ (. f (open "GET" fl #_false true))
         _ (println fl)
         _ (set! (.-onreadystatechange f) 
             (fn []
               (if (= (.-readyState f) 4) ;done
                 (do (println :inside!)
                   (if (or (= (.-status f) 200)
                           (= (.-status f) 0))
                     (do
                       (println [:status (.-status f)
                                 :response (.-responseText f)])
                       (reset! res (.-responseText f))))))))
         _ (. f send nil)]            
     #_@res    
     #_(.-responseText f)
     (await! res 100)
     @res))
            

#_(defn file->lines-sync [fl]
    (let [the-result (atom nil)
          rdr (->file-reader
                (fn [_]
                  (this-as this
                    (let [txt (.-result this)]
                      ; _ (log! txt)
                      
                      ; (async/put! res txt)
                      (reset! the-result txt)))))
          _   (read-file! rdr fl)]        
      (await! the-result 100)))

