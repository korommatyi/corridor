(ns corridor.board
  (:import (java.lang Math))
  (:require [clojure.set :refer [difference]]))

(def board-size 9)  ; coordinates run from 1 to board-size
(def num-of-walls 10)

(defrecord Position [figure walls])
(defrecord Board [player1 player2])
(defrecord WallCoordinate [x y alignment])
; (1 1 :vertival) -> it lies between the (1 1) and (2 1) from (1 2) and (2 2)

(defn- other-player [player]
  (if (= :player1 player) :player2 :player1))

(defn new-board []
  (let [center (+ 1 (/ (- board-size 1) 2))]
    (Board. (Position. [1 center] []) (Position. [board-size center] []))))

(defn- walls-on-board [board]
  (concat (:walls (:player1 board)) (:walls (:player2 board))))

(defn- can-place-wall? [board player]
  (>= num-of-walls (count (:walls (player board)))))

(defn neighbors [x y]
  (let [in-bounds (fn [c] (and (<= c board-size) (>= c 1)))]
    (for [dx [-1 0 1]
          dy [-1 0 1]
          :let [x' (+ x dx)
                y' (+ y dy)]
          :when (and (not (= (Math/abs dx) (Math/abs dy)))
                     (in-bounds x')
                     (in-bounds y'))]
      [x' y'])))

(defn- vertically-connected? [[_ y1] [_ y2]]
  (= 1 (Math/abs (- y1 y2))))

(defn- horisontally-connected? [[x1 _] [x2 _]]
  (= 1 (Math/abs (- x1 x2))))

(defn- connection-type [c1 c2]
  (if (vertically-connected? c1 c2)
    :vertical
    (if (horisontally-connected? c1 c2)
      :horizontal
      :nothing)))

(defn blocks? [[x1 y1 :as c1] [x2 y2 :as c2] wall]
  {:pre [(or (vertically-connected? c1 c2) (horisontally-connected? c1 c2))]}
  (case [(connection-type c1 c2) (:alignment wall)]
    [:vertical :horizontal] (and (= (min y1 y2) (:y wall)) (#{0 1} (- x1 (:x wall))))
    [:horizontal :vertical] (and (= (min x1 x2) (:x wall)) (#{0 1} (- y1 (:y wall))))
    false))

(defn moves [player board]
  (let [[x y :as p1] (:figure (player board))
        ns (neighbors x y)
        walls (walls-on-board board)
        blocked? (fn [p2] (some boolean (map #(blocks? p1 p2 %) walls)))]
    (filter #(not (blocked? %)) ns)))

(def all-possible-wall-positions
  (set (for [x (range 1 board-size)
             y (range 1 board-size)]
         [x y])))

(defn wall-placements [player board]
  (if (can-place-wall? board player)
    (let [walls (walls-on-board board)
          wall-positions (set (map #(identity [(:x %) (:y %)]) walls))]
      (difference all-possible-wall-positions wall-positions))
    #{}))
