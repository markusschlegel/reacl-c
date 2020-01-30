(ns ^:no-doc reacl-c.base)

(defprotocol E)

(defn element? [v]
  (or (string? v) (satisfies? E v)))

(defn lens? [v]
  (or (ifn? v) (keyword? v) (integer? v)))

(defprotocol Ref
  (-deref-ref [this]))

(defrecord Fragment [children] E)

(defrecord Dynamic [f args] E) ;; aka WithState
(defrecord WithAsyncActions [f args] E)
(defrecord WithRef [f args] E)

(defrecord Focus [e lens] E)
(defrecord LocalState [e initial] E)

(defrecord HandleAction [e f] E)
(defrecord SetRef [e ref] E)
(defrecord DidUpdate [e f] E)
(defrecord CaptureStateChange [e f] E)
(defrecord HandleMessage [e f] E)
(defrecord Named [e name] E)
(defrecord ErrorBoundary [e f] E)
(defrecord Keyed [e key] E)

(defrecord DidMount [return] E)
(defrecord WillUnmount [return] E)

(defn fragment? [v]
  (instance? Fragment v))

(defrecord Returned [opt-state actions messages])

(defn returned? [v] (instance? Returned v))

(defn merge-returned [r1 & rs]
  (loop [r1 r1
         rs rs]
    (if (empty? rs)
      r1
      (let [r2 (first rs)
            rm (Returned. (or (:opt-state r2) (:opt-state r1))
                          (vec (concat (:actions r1) (:actions r2)))
                          (vec (concat (:messages r1) (:messages r2))))]
        (recur rm (rest rs))))))

(defprotocol Application
  (-send-message! [this msg]))
