(ns corridor.board
  (:import (java.lang Math))
  (:require [clojure.set :refer [difference]]))

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

(defn new-board []
  (let [center (+ 1 (/ (- board-size 1) 2))]
    (Board.
      0
      (Position. [1 center] [])
      (Position. [board-size center] [])
      (into {} (for [x (range 1 (+ 1 board-size))
                     y (range 1 (+ 1 board-size))]
                 [[x y] (cell x y)])))))


(defn connections-of [board coord]
  (:neighbors ((:cells board) coord)))

(defn- walls-on-board [board]
  (concat (:walls (:player1 board)) (:walls (:player2 board))))

(defn- can-place-wall? [board player]
  (>= num-of-walls (count (:walls (player board)))))

(defn- whose-turn? [board]
  (if (even? (:turn board)) :player1 :player2))


(defn move
  ([board coord]
   (let [player (whose-turn? board)]
     (move board player coord)))

  ([board player coord]
   (-> board
       (update :turn inc)
       (update-in [player :figure] (fn [_] coord)))))


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

(defn- separate [board wall-coord]
  (let [[p1 p2] (which-pairs-does-it-separate wall-coord)
        [a b] p1
        [c d] p2]
    (-> board
        (close-connection a b)
        (close-connection c d))))

(defn place-wall
  ([board wall-coord]
   (let [player (whose-turn? board)]
     (place-wall board player wall-coord)))

  ([board player wall-coord]
   (-> board
       (update :turn inc)
       (update-in [player :walls] (fn [v] (conj v wall-coord)))
       (separate wall-coord))))
