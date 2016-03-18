(ns corridor.games
  (:require [corridor.board :refer [new-board whose-turn?]]))

(defrecord Game [id board player1 player2 next-to-move])

(defn new-game [id player1 player2]
  (let [board (new-board)]
    (Game. id board player1 player2 (whose-turn? board))))

(defrecord ^{:private true} GameRegistry [last-id registry])

(defn game-registry []
  (GameRegistry. -1 {}))

(defn register-new-game [game-registry player1 player2]
  (let [id (inc (:last-id game-registry))
        game (new-game id player1 player2)]
    [id (GameRegistry. id (assoc (:registry game-registry) id game))]))

(defn delete-game [game-registry id]
  (GameRegistry. (:last-id game-registry) (dissoc (:registry game-registry) id)))

(defn get-game [game-registry id]
  ((:registry game-registry) id))
