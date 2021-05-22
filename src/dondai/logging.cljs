(ns dondai.logging)

(defn get-logger-internal [name logfn]
  (fn [& args]
    (apply logfn name (map #(if (string? %) % (prn-str %)) args))))

(defn get-logger [name logfn]
  (fn [& args]
    (apply (.-log js/console) name (map #(if (string? %) % (prn-str %)) args))))

