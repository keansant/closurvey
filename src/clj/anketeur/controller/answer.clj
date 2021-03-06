(ns anketeur.controller.answer
  (:require
    [clojure.tools.logging :as log]
    [ashikasoft.webstack.layout :as layout]
    [anketeur.survey :as survey]
    [anketeur.view.answer :as view.answer]
    [ring.util.http-response :as response]))

(defn render-opener [ds]
  (let [doclist (survey/query-docs ds (complement :deleted?))
        errors (when (empty? doclist)
                  ["No documents found. Please create a new document."])]
    (layout/render-hiccup
      view.answer/opener
      {:glossary {:title "Respond to a Survey"}
       :flash-errors errors
       :open-link-base "/answer/id/"
       :doclist doclist})))

(defn render-add [ds surveyno]
  (let [survey-info (survey/read-doc ds surveyno)]
    (log/info "surveyno: " surveyno "survey-info: " survey-info)
    (layout/render-hiccup
      view.answer/add
      {:survey-info (select-keys survey-info [:surveyno :surveyname :description])
       :flash-errors (when-not survey-info "Error: Unable to open survey for answering.")
       :glossary {:title "Survey"}})))

(defn add-action [ds {:keys [params] :as request}]
  (let [{:keys [surveyno]} params
        survey-info (when surveyno (survey/read-doc ds surveyno))
        formno (when survey-info (survey/save-answers! ds surveyno {:answers {}}))]
    (log/info "surveyno: " surveyno "formno:" formno)
    (if formno
      (-> (response/see-other (str "/answer/id/" surveyno "/formno/" formno)))
      (-> (response/internal-server-error "Error: Unable to open survey for answering.")))))

;; TODO sanitize surveyno formno
(defn render-responder-nojs [ds surveyno formno]
  (let [survey-info (survey/read-doc ds surveyno)
        form (survey/read-answer-form ds surveyno formno)]
    (log/info " nojs surveyno: " surveyno "form:" form)
    (layout/render-hiccup
      view.answer/responder-nojs
      {:survey-info (assoc-in survey-info [:answers :formno] (when form formno))
       :surveyno surveyno
       :formno formno
       :flash-errors (when-not form "Error: Unable to open survey for answering.")
       :glossary {:title "Survey"}})))

(defn render-responder [ds surveyno formno]
  (let [survey-info (survey/read-doc ds surveyno)
        form (survey/read-answer-form ds surveyno formno)]
    (log/info "surveyno: " surveyno "form:" form)
    (layout/render-hiccup
      view.answer/responder
      {:survey-info (assoc-in survey-info [:answers :formno] (when form formno))
       :surveyno surveyno
       :formno formno
       :flash-errors (when-not form "Error: Unable to open survey for answering.")
       :glossary {:title "Survey"}})))

;; TODO sanitize/validate form data
(defn completed [ds surveyno]
  (let [survey-info (survey/read-doc ds surveyno)]
    (log/info "surveyno: " surveyno)
    (layout/render-hiccup
      view.answer/show-message
      {:survey-info survey-info
       :message "Thank you."
       :glossary {:title "Survey"}})))

;; TODO sanitize/validate form data
(defn answer-action [ds {:keys [params] :as request}]
  (let [{:keys [surveyno answers]} params
        save-status (survey/save-answers! ds surveyno {:answers answers})]
    (log/info "save answers: " answers " params: " params " request: " request)
    (if save-status
      (response/ok "Form saved.")
      (response/internal-server-error "Internal error: unable to save form."))))

;; TODO sanitize/validate form data
(defn answernojs-action [ds {:keys [params] :as request}]
  (let [answers (dissoc params :__anti-forgery-token :surveyno)
        {:keys [surveyno formno]} params
        save-status (survey/save-answers! ds surveyno {:answers answers})]
    (log/info "surveyno: " surveyno "answers: " answers)
    (if save-status
      (-> (response/see-other (str "/answer/completed/" surveyno)))
      (response/internal-server-error "Internal error: unable to save form."))))

