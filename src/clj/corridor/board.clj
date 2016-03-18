(ns corridor.board
  (:import (java.lang Math))
  (:require [clojure.set :refer [difference]]
            [clojure.data.priority-map :refer [priority-map]]))

(def board-size 9)  ; coordinates run from 1 to board-size
(def num-of-walls 10)

(defrecord Position [figure walls])
(defrecord Cell [x y neighbors])
(defrecord Board [turn player1 player2 cells])
(defrecord WallCoordinate [x y alignment])
; (WallCoordinate. 1 1 :vertival) -> it separates (1 1) and (2 1) from (1 2) and (2 2)

(defn- neighbors [x y]
  (let [in-bounds (fn [c] (and (<= c board-size) (>= c 1)))]
    (for [dx [-1 0 1]
          dy [-1 0 1]
          :let [x' (+ x dx)
                y' (+ y dy)]
          :when (and (not (= (Math/abs dx) (Math/abs dy)))
                     (in-bounds x')
                     (in-bounds y'))]
      [x' y'])))

(defn- cell [x y]
  (Cell. x y (neighbors x y)))

(def cell-range (range 1 (+ 1 board-size)))
(def wall-range (range 1 board-size))

(defn new-board []
  (let [center (+ 1 (/ (- board-size 1) 2))]
    (Board.
      0
      (Position. [center 1] [])
      (Position. [center board-size] [])
      (into {} (for [x cell-range
                     y cell-range]
                 [[x y] (cell x y)])))))


(defn connections-of [board coord]
  (:neighbors ((:cells board) coord)))

(defn- walls-on-board [board]
  (concat (:walls (:player1 board)) (:walls (:player2 board))))

(defn can-place-wall? [board player]
  (>= num-of-walls (count (:walls (player board)))))

(defn whose-turn? [board]
  (if (even? (:turn board)) :player1 :player2))

(def other-player {:player1 :player2
                   :player2 :player1})


(defn move
  ([board coord] (move board (whose-turn? board) coord))

  ([board player coord]
   (-> board
       (update :turn inc)
       (update-in [player :figure] (fn [_] coord)))))

(defn possible-moves
  ([board] (possible-moves board (whose-turn? board)))

  ([board player]
   (let [this-pos (:figure (player board))
         other-pos (:figure ((other-player player) board))
         this-neighbors (connections-of board this-pos)]
     (if-not (some #{other-pos} this-neighbors)
       this-neighbors
       (let [other-neighbors (connections-of board other-pos)
             union (into this-neighbors other-neighbors)]
         (filter #(not (#{this-pos other-pos} %)) union))))))


(defn- which-pairs-does-it-separate [{:keys [x y alignment]}]
  (let [base [x y]
        up [x (+ y 1)]
        right [(+ x 1) y]
        right-up [(+ x 1) (+ y 1)]]
    (if (= :horizontal alignment)
      [[base up] [right right-up]]
      [[base right] [up right-up]])))

(defn- close-connection [board cell1 cell2]
  (-> board
      (update-in [:cells cell1 :neighbors] (fn [v] (remove #(= cell2 %) v)))
      (update-in [:cells cell2 :neighbors] (fn [v] (remove #(= cell1 %) v)))))

(defn- separate-by-wall [board wall-coord]
  (let [[p1 p2] (which-pairs-does-it-separate wall-coord)
        [a b] p1
        [c d] p2]
    (-> board
        (close-connection a b)
        (close-connection c d))))

(defn place-wall
  ([board wall-coord] (place-wall board (whose-turn? board) wall-coord))

  ([board player wall-coord]
   (-> board
       (update :turn inc)
       (update-in [player :walls] (fn [v] (conj v wall-coord)))
       (separate-by-wall wall-coord))))


(defn- construct-path [from to parent]
  (loop [path []
         current to]
    (let [path' (conj path current)]
      (if (= from current)
        (reverse path')
        (recur path' (parent current))))))

(defn get-path [board from-coord to-y]
  (let [priority (fn [[_ y]] (Math/abs (- y to-y)))
        cells (:cells board)]
    (loop [finished #{}
           reached (priority-map from-coord (priority from-coord))
           parent {}]
      (if-let [[active-node p] (peek reached)]
        (if (= 0 p)
          (construct-path from-coord active-node parent)
          (let [neighbors (:neighbors (cells active-node))
                new-nodes (filter #(not (or (finished %) (reached %))) neighbors)
                finished' (conj finished active-node)
                reached' (into (pop reached) (map #(identity [% (priority %)]) new-nodes))
                parent' (into parent (map #(identity [% active-node]) new-nodes))]
            (recur finished' reached' parent')))
        nil))))

(defn- wall-on-righ [x y] (WallCoordinate. (+ x 1) y :horizontal))
(defn- wall-on-left [x y] (WallCoordinate. (- x 1) y :horizontal))
(defn- wall-below [x y] (WallCoordinate. x (- y 1) :vertical))
(defn- wall-above [x y] (WallCoordinate. x (+ y 1) :vertical))

(defn- walls-physically-blocking [x y alignment]
  (let [walls-on-same-place [(WallCoordinate. x y :vertical) (WallCoordinate. x y :horizontal)]]
    (if (= alignment :horizontal)
      (into walls-on-same-place [(wall-on-left x y) (wall-on-righ x y)])
      (into walls-on-same-place [(wall-above x y) (wall-below x y)]))))

(defn- physically-possible-walls [board]
  (let [existing-walls (set (walls-on-board board))]
    (for [x wall-range
          y wall-range
          alignment [:horizontal :vertical]
          :let [blockers (walls-physically-blocking x y alignment)]
          :when (every? #(not (existing-walls %)) blockers)]
      (WallCoordinate. x y alignment))))

(defn blocks-path? [wall path]
  (let [steps (map set (partition 2 1 path))
        blocker (set (map set (which-pairs-does-it-separate wall)))
        step-blocked? (fn [step] (blocker step))]
    (some step-blocked? steps)))

(defn- does-not-block-path? [wall path]
  (if (not (blocks-path? wall path)) path))

(defn- get-independent-path [from to-y previous-paths-to-y wall board]
  (if-let [path (some #(does-not-block-path? wall %) previous-paths-to-y)]
    path
    (get-path (place-wall board wall) from to-y)))

(defn- wall-checker [board figure goal]
  (let [cached-paths (atom #{})]
    (fn [wall]
      (let [path (get-independent-path figure goal @cached-paths wall board)]
        (if path
          (do
            (swap! cached-paths conj path)
            wall))))))

(defn valid-walls [board]
  (let [p1 (:figure (:player1 board))
        p2 (:figure (:player2 board))
        walls (vec (physically-possible-walls board))
        leaves-possible-to-reach-top? (wall-checker board p1 9)
        levaes-possible-to-reach-bottom? (wall-checker board p2 1)]
    (filter #(and (leaves-possible-to-reach-top? %) (levaes-possible-to-reach-bottom? %)) walls)))
