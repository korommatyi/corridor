(ns corridor.board-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [corridor.board :refer :all]
            [clojure.set :refer [difference]])
  (:import (corridor.board WallCoordinate Position Board)))

(deftest test-neighbors
  (testing "neighbors of (1, 1)"
    (let [output (neighbors 1 1)]
      (is (= [[1 2] [2 1]] (vec output)))))

  (testing "neighbors of (2, 2)"
    (let [output (neighbors 2 2)]
      (is (= [[1 2] [2 1] [2 3] [3 2]] (vec output))))))

(deftest test-blocks
  (testing "wall [1 1 :horizontal] blocks [1 1] to [1 2]"
    (let [output (blocks? [1 1] [1 2] (WallCoordinate. 1 1 :horizontal))]
      (is (= true (boolean output)))))

  (testing "wall [1 1 :horizontal] blocks [2 1] to [2 2]"
    (let [output (blocks? [2 1] [2 2] (WallCoordinate. 1 1 :horizontal))]
      (is (= true (boolean output)))))

  (testing "wall [1 1 :vertical] blocks [1 1] [2 1]"
    (let [output (blocks? [1 1] [2 1] (WallCoordinate. 1 1 :vertical))]
      (is (= true (boolean output)))))

  (testing "wall [1 1 :vertical] blocks [1 2] [2 2]"
    (let [output (blocks? [1 2] [2 2] (WallCoordinate. 1 1 :vertical))]
      (is (= true (boolean output)))))

  (testing "wall [1 1 :vertical] does not block [1 1] to [1 2]"
    (let [output (blocks? [1 1] [1 2] (WallCoordinate. 1 1 :vertical))]
      (is (= false (boolean output)))))

  (testing "wall [1 1 :horizontal] does not block [1 1] [2 1]"
    (let [output (blocks? [1 1] [2 1] (WallCoordinate. 1 1 :horizontal))]
      (is (= false (boolean output)))))

  (testing "wall [1 1 :horizontal] does not block [3 1] to [3 2]"
    (let [output (blocks? [3 1] [3 2] (WallCoordinate. 1 1 :horizontal))]
      (is (= false (boolean output)))))

  (testing "wall [1 1 :vertical] does not block [3 1] [3 2]"
    (let [output (blocks? [3 1] [3 2] (WallCoordinate. 1 1 :vertical))]
      (is (= false (boolean output)))))
  )

(deftest test-moves
  (testing "moves"
    (let [position1 (Position. [5 5] [(WallCoordinate. 5 5 :horizontal)])
          position2 (Position. [1 1] [(WallCoordinate. 2 2 :vertical)])
          board (Board. position1 position2)]
      (is (= [[4 5] [5 4] [6 5]] (vec (moves :player1 board)))))))

(deftest test-wall-placements
  (testing "wall-palcements"
    (let [e (difference all-possible-wall-positions #{[2 2] [5 5]})
          position1 (Position. [5 5] [(WallCoordinate. 5 5 :horizontal)])
          position2 (Position. [1 1] [(WallCoordinate. 2 2 :vertical)])
          board (Board. position1 position2)]
      (is (= e (wall-placements :player1 board))))))
