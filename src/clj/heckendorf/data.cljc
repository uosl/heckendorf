(ns heckendorf.data
  #?(:clj (:require [clojure.java.io :as io])))

(def weapon-keys {"s" 0, "d" 1, "f" 2, "g" 3})
(def potion-keys {"c" 0, "v" 1, "b" 2})
(def hotkeys
  (into #{; rest
          "r"
          ; movement with vim bindings
          "h" "j" "k" "l" "n" "m" "i" "o"
          ; diagonal movement and rest with numpad
          "Home" "PageUp"
          "Clear"
          "End" "PageDown"
          ; movement and rest with numpad (numlock active)
          "7" "8" "9"
          "4" "5" "6"
          "1" "2" "3"
          ; arrow keys
          "ArrowLeft" "ArrowRight" "ArrowUp" "ArrowDown"}
        (concat (keys weapon-keys)
                (keys potion-keys))))

(def materials
  {:common {:wood 0.6 :stone 1}
   :uncommon {:iron 2 :steel 2.2}
   :rare {:mithril 3.1 :adamant 3.8}
   :epic {:obsidian 4.5 :abyssal 5.3}})

(def grades
  "Map of all `materials` to their grades."
  (apply merge (vals materials)))

(defn inv->pots [inv]
  (->> inv
       (group-by :type)
       :potion
       (map :grade)
       frequencies
       (reduce-kv
         (fn [m k v]
           (assoc m
                  (case k :minor "c" :lesser "v" :greater "b")
                  [v k]))
         (sorted-map-by #(< (get potion-keys %1 -1) (get potion-keys %2 -1))))))

(defn inv->weps [inv]
  (->> inv
       (group-by :type)
       :weapon
       (group-by :form)
       (reduce-kv
         (fn [m k v]
           (assoc m
                  (case k :sword "s" :dagger "d" :mace "f" :greatsword "g")
                  [(apply max-key grades (map :grade v)) k]))
         (sorted-map-by #(< (get weapon-keys %1 -1) (get weapon-keys %2 -1))))))

(defn hotkey->index [inv hotkey]
  (let [pot (some-> (inv->pots inv) (get hotkey))
        wep (some-> (inv->weps inv) (get hotkey))]
    (if-some [item (cond
                     (some? pot)
                     {:type :potion, :grade (second pot)}
                     (some? wep)
                     {:type :weapon, :form (second wep), :grade (first wep)})]
      (->> (map-indexed vector inv)
           (filter #(= (second %) item))
           first
           first)
      nil)))

(defn bounded-conj
  "Drops the oldest element in bounded-vector when length is more than or equal
  to limit key specified in metadata. Defaults to a limit of 10 when metadata
  is missing."
  [bounded-vector x]
  (let [{:keys [limit] :as m} (meta bounded-vector)]
    (conj (cond-> bounded-vector
                  (>= (count bounded-vector) (or limit 10))
                  (-> (subvec 1)
                      (with-meta (or m {:limit 10}))))
          x)))

(defn item->vec [item]
  (case (:type item)
    :weapon ((juxt :grade :form) item)
    :potion ((juxt :grade :type) item)))

#?(:clj (defn file [f]
          (io/file (System/getProperty "user.dir") f)))
