(ns closurvey.app
  (:require
    [closurvey.core :as core]
    [closurvey.opener :as opener]
    [closurvey.client.result :as result]
    [closurvey.responder :as responder]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))
