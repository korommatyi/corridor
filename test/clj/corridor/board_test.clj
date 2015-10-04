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
    (let [board (new-board)
          altered-board (move board :player1 [3 3])]
      (is (= [3 3] (:figure (:player1 altered-board)))))))

(deftest test-place-wall
  (testing "place horizontal wall to [2 2]"
    (let [board (new-board)
          altered-board (place-wall board :player1 (WallCoordinate. 2 2 :horizontal))
          ns2-2 (vec (connections-of altered-board [2 2]))
          ns3-2 (vec (connections-of altered-board [3 2]))
          ns2-3 (vec (connections-of altered-board [2 3]))
          ns3-3 (vec (connections-of altered-board [3 3]))]
      (is (= [[1 2] [2 1] [3 2]] ns2-2))
      (is (= [[2 2] [3 1] [4 2]] ns3-2))
      (is (= [[1 3] [2 4] [3 3]] ns2-3))
      (is (= [[2 3] [3 4] [4 3]] ns3-3))
      (is (= 1 (count (:walls (:player1 altered-board)))))))

  (testing "place vertical wall to [2 2]"
    (let [board (new-board)
          altered-board (place-wall board :player1 (WallCoordinate. 2 2 :vertical))
          ns2-2 (vec (connections-of altered-board [2 2]))
          ns3-2 (vec (connections-of altered-board [3 2]))
          ns2-3 (vec (connections-of altered-board [2 3]))
          ns3-3 (vec (connections-of altered-board [3 3]))]
      (is (= [[1 2] [2 1] [2 3]] ns2-2))
      (is (= [[3 1] [3 3] [4 2]] ns3-2))
      (is (= [[1 3] [2 2] [2 4]] ns2-3))
      (is (= [[3 2] [3 4] [4 3]] ns3-3))
      (is (= 1 (count (:walls (:player1 altered-board))))))))
