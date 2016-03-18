(ns corridor.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.reload :as reload]
            [ring.util.response :refer [redirect]]
            [org.httpkit.server :refer [run-server]]))

(defroutes app-routes
  (GET "/" [] (redirect "index.html"))
  (route/not-found "Not Found"))

(def app (wrap-defaults app-routes site-defaults))

(defn in-dev? [args] true) ;; TODO read a config variable from command line, env, or file?

(defn -main [& args]
  (let [handler (if (in-dev? args)
                  (reload/wrap-reload app)
                  app)]
    (println "Start server!")
    (run-server handler {:port 8080})))
