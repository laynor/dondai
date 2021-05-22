(ns dondai.logging)
(defmacro defloggers [name]
  `(do (def ~(symbol "log") (get-logger-internal ~name (.-log js/console)))
       (def ~(symbol "logi") (get-logger-internal ~name (.-info js/console)))
       (def ~(symbol "logw") (get-logger-internal ~name (.-warn js/console)))
       (def ~(symbol "loge") (get-logger-internal ~name (.-error js/console)))))
