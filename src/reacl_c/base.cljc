(ns ^:no-doc reacl-c.base
    (:require #?(:cljs [active.clojure.cljs.record :as r :include-macros true])
              #?(:clj [active.clojure.record :as r])))

(defprotocol E)

(defn item? [v]
  (or (string? v)
      (nil? v)
      (satisfies? E v)))

(defn- item-list-pred [v]
  ;; nil for easier 'conditional rendering'
  (or (nil? v) (item? v)))

(defn assert-item-list [where cs] ;; -> more like a util?
  (or (every? item-list-pred cs)
      (let [c (first (filter #(not (item-list-pred %)) cs))]
        (assert false
                (str "Expected only items or strings, but this is neither: " (pr-str c)))
        false)))

(defn assert-item [c] ;; -> more like a util?
  (or (item? c)
      (do
        (assert false
                (str "Expected an item or string, but this is neither: " (pr-str c)))
        false)))

(defprotocol Ref
  (-deref-ref [this]))
(defn ref? [v]
  (satisfies? Ref v))

(deftype NameId [name])

(defn make-name-id [s]
  {:pre [(string? s)]}
  (NameId. s))

(defn name-id? [v]
  (instance? NameId v))

(defn name-id-name [^NameId v]
  {:pre [(name-id? v)]}
  (.-name v))

(r/define-record-type Fragment
  (really-make-fragment children)
  really-fragment?
  [children fragment-children]
  E)

(defn make-fragment [children]
  (if (empty? children)
    nil
    (really-make-fragment children)))

(defn fragment? [v]
  ;; nil is an empty fragment
  (or (nil? v) (really-fragment? v)))

(r/define-record-type Dynamic
  (make-dynamic f args)
  dynamic?
  [f dynamic-f
   args dynamic-args]
  E)

(r/define-record-type Static
  (make-static f args)
  static?
  [f static-f
   args static-args]
  E)

(r/define-record-type WithRef
  (make-with-ref f args)
  with-ref?
  [f with-ref-f
   args with-ref-args]
  E)

(r/define-record-type WithAsyncReturn
  (make-with-async-return f args)
  with-async-return?
  [f with-async-return-f
   args with-async-return-args]
  E)

(r/define-record-type Focus
  (make-focus e lens)
  focus?
  [e focus-e
   lens focus-lens]
  E)

(r/define-record-type LocalState
  (make-local-state e initial)
  local-state?
  [e local-state-e
   initial local-state-initial]
  E)

(r/define-record-type HandleAction
  (make-handle-action e f)
  handle-action?
  [e handle-action-e
   f handle-action-f]
  E)

(r/define-record-type SetRef
  (make-set-ref e ref)
  set-ref?
  [e set-ref-e
   ref set-ref-ref]
  E)

(r/define-record-type HandleStateChange
  (make-handle-state-change e f)
  handle-state-change?
  [e handle-state-change-e
   f handle-state-change-f]
  E)

(r/define-record-type HandleMessage
  (make-handle-message f e)
  handle-message?
  [f handle-message-f
   e handle-message-e]
  E)

(r/define-record-type Named
  (make-named name-id e)
  named?
  [name-id named-name-id
   e named-e]
  E)

(r/define-record-type ErrorBoundary
  (make-error-boundary e f)
  error-boundary?
  [e error-boundary-e
   f error-boundary-f]
  E)

(r/define-record-type Keyed
  (make-keyed e key)
  keyed?
  [e keyed-e
   key keyed-key]
  E)

(r/define-record-type Livecycle
  (make-livecycle init finish)
  livecycle?
  [init livecycle-init
   finish livecycle-finish]
  E)


(defn message-target? [v]
  (or (ref? v)
      (set-ref? v)))

(defn deref-message-target [target]
  (-deref-ref (if (set-ref? target)
                (set-ref-ref target)
                target)))


(defrecord KeepState [])
(def keep-state (KeepState.))

(r/define-record-type Returned
  (make-returned state actions messages)
  returned?
  [state returned-state
   actions returned-actions
   messages returned-messages])

(def empty-return (make-returned keep-state [] []))

(defn merge-returned
  [r1 & rs]
  {:pre [(returned? r1)
         (every? returned? rs)]}
  (loop [r1 r1
         rs rs]
    (if (empty? rs)
      r1
      (let [r2 (first rs)
            rm (make-returned (if (not= keep-state (:state r2)) (:state r2) (:state r1))
                              (vec (concat (:actions r1) (:actions r2)))
                              (vec (concat (:messages r1) (:messages r2))))]
        (recur rm (rest rs))))))

(defprotocol Application
  (-send-message! [this msg]))

(r/define-record-type Effect
  (make-effect f args)
  effect?
  [f effect-f
   args effect-args])

(defn run-effect!
  "Returns a tuple [value ret]. If an effect returnn a [[return]]
  value, then 'value' is the returned state, and 'ret' everything else.
  For any other value, 'ret' is empty."
  [eff]
  {:pre [(effect? eff)]}
  (let [result (apply (:f eff) (:args eff))]
    (if (returned? result)
      (if (not= keep-state (:state result))
        [(:state result) (update result :state keep-state)]
        [nil result])
      [result empty-return])))
