(ns corridor.board-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [corridor.board :refer :all]
            [clojure.set :refer [difference]])
  (:import (corridor.board WallCoordinate)))

(deftest test-new-board
  (testing "neighbors of [1 1]"
    (let [board (new-board)
          ns (vec (connections-of board [1 1]))]
      (is (= [[1 2] [2 1]] ns))))

  (testing "neighbors of [2 2]"
    (let [board (new-board)
          ns (vec (connections-of board [2 2]))]
      (is (= [[1 2] [2 1] [2 3] [3 2]] ns)))))

(deftest test-move
  (testing "move player 1 to [3 3]"
    (let [board (move (new-board) [3 3])]
      (is (= [3 3] (:figure (:player1 board)))))))

(deftest test-place-wall
  (testing "place horizontal wall to [2 2]"
    (let [board (place-wall (new-board) (WallCoordinate. 2 2 :horizontal))
          ns2-2 (vec (connections-of board [2 2]))
          ns3-2 (vec (connections-of board [3 2]))
          ns2-3 (vec (connections-of board [2 3]))
          ns3-3 (vec (connections-of board [3 3]))]
      (is (= [[1 2] [2 1] [3 2]] ns2-2))
      (is (= [[2 2] [3 1] [4 2]] ns3-2))
      (is (= [[1 3] [2 4] [3 3]] ns2-3))
      (is (= [[2 3] [3 4] [4 3]] ns3-3))
      (is (= 1 (count (:walls (:player1 board)))))))

  (testing "place vertical wall to [2 2]"
    (let [board (place-wall (new-board) (WallCoordinate. 2 2 :vertical))
          ns2-2 (vec (connections-of board [2 2]))
          ns3-2 (vec (connections-of board [3 2]))
          ns2-3 (vec (connections-of board [2 3]))
          ns3-3 (vec (connections-of board [3 3]))]
      (is (= [[1 2] [2 1] [2 3]] ns2-2))
      (is (= [[3 1] [3 3] [4 2]] ns3-2))
      (is (= [[1 3] [2 2] [2 4]] ns2-3))
      (is (= [[3 2] [3 4] [4 3]] ns3-3))
      (is (= 1 (count (:walls (:player1 board))))))))

(deftest test-possible-moves
  (testing "possible moves on a new board"
    (let [board (new-board)
          moves (vec (possible-moves board :player1))]
      (is (= [[4 1] [5 2] [6 1]] moves))))

  (testing "possible moves from [5 1] if opponent stand on [5 2]"
    (let [board (move (new-board) :player2 [5 2])
          moves (vec (possible-moves board :player1))]
      (is (= [[6 2] [5 3] [4 2] [4 1] [6 1]] moves)))))

(deftest test-blocks-path
  (testing "Wall [2 1 :vertical] blocks path [1 1] <-> [2 1] <-> [3 1]"
    (let [wall (WallCoordinate. 2 1 :vertical)
          path [[1 1] [2 1] [3 1]]]
      (is (boolean (blocks-path? wall path)))
      (is (boolean (blocks-path? wall (reverse path))))))

  (testing "Wall [2 1 :horizontal] does not block path [1 1] -> [2 1] -> [3 1]"
    (let [wall (WallCoordinate. 2 1 :horizontal)
          path [[1 1] [2 1] [3 1]]]
      (is (not (boolean (blocks-path? wall path)))))))

(deftest test-path
  (testing "Path from [1 1] to [_ 9] on an empty board"
    (let [p (get-path (new-board) [1 1] 9)]
      (is (= [[1 1] [1 2] [1 3] [1 4] [1 5] [1 6] [1 7] [1 8] [1 9]] p))))

  (testing "Path from [1 1] to [_ 9] on a board with walls [[1 1 :horizontal]]"
    (let [board (place-wall (new-board) (WallCoordinate. 1 1 :horizontal))
          p (get-path board [1 1] 9)]
      (is (= [[1 1] [2 1] [3 1] [3 2] [3 3] [3 4] [3 5] [3 6] [3 7] [3 8] [3 9]] p))))

  (testing "No path from [1 1] to [_ 9] on a board with walls [[1 1 :horizontal] [2 1 :vertical]]"
    (let [board (-> (new-board)
                    (place-wall (WallCoordinate. 1 1 :horizontal))
                    (place-wall (WallCoordinate. 2 1 :vertical)))
          p (get-path board [1 1] 9)]
      (is (nil? p)))))

(deftest test-valid-walls
  (testing "Every wall is valid on an empty board"
    (let [walls (vec (valid-walls (new-board)))]
      (is (= 128 (count walls)))))

  (testing "Can not place wall on existing one."
    (let [board (place-wall (new-board) (WallCoordinate. 8 1 :horizontal))
          walls (set (valid-walls board))]
      (is (= 125 (count walls)))
      (is (nil? (walls (WallCoordinate. 8 1 :horizontal))))
      (is (nil? (walls (WallCoordinate. 8 1 :vertical))))
      (is (nil? (walls (WallCoordinate. 7 1 :horizontal))))))

  (testing "Can not place wall on existing one."
    (let [board (place-wall (new-board) (WallCoordinate. 2 2 :vertical))
          walls (set (valid-walls board))]
      (is (= 124 (count walls)))
      (is (nil? (walls (WallCoordinate. 2 2 :horizontal))))
      (is (nil? (walls (WallCoordinate. 2 2 :vertical))))
      (is (nil? (walls (WallCoordinate. 2 3 :vertical))))
      (is (nil? (walls (WallCoordinate. 2 1 :vertical))))))

  (testing "Can not place wall that separates a player from the goal"
    (let [board (-> (new-board)
                    (place-wall (WallCoordinate. 1 1 :horizontal))
                    (place-wall (WallCoordinate. 3 1 :horizontal))
                    (place-wall (WallCoordinate. 5 1 :horizontal)))
          walls (set (valid-walls board))]
      (is (= 118 (count walls)))
      (is (nil? (walls (WallCoordinate. 6 1 :vertical)))))))
