(ns corridor.games-test
  (:require [clojure.test :refer :all]
            [corridor.games :refer :all]))

(deftest test-register-game
  (testing "registered game is retrievable"
    (let [registry (game-registry)
          [id registry'] (register-new-game registry :1 :2)
          game (get-game registry' id)]
      (is (= id (:id game)))))

  (testing "as many game registered as many times register is called"
    (let [registry (game-registry)
          register-games (fn [n r]
                           (if (<= n 0)
                             r
                             (recur (- n 1) ((register-new-game r :1 :2) 1))))
          registry' (register-games 10 registry)]
      (is (= 10 (count (:registry registry'))))))

  (testing "arguments are properly propagated"
    (let [[id r] (register-new-game (game-registry) :1 :2)
          game (get-game r id)]
      (is (= :1 (:player1 game)))
      (is (= :2 (:player2 game))))))

(deftest test-delete-game
  (testing "deleted game is gone"
    (let [[id r] (register-new-game (game-registry) :1 :2)
          r' (delete-game r id)]
      (is (not ((:registry r') id)))))

  (testing "other games are still there"
    (let [[id1 r] (register-new-game (game-registry) :1 :2)
          [id2 r'] (register-new-game r :1 :2)
          r'' (delete-game r' id1)]
      (is ((:registry r'') id2)))))
