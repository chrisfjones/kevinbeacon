(ns kevinbeacon-service.sifter-parse
  (:require [clojure.string :as str]))

(def sifter-keys
  (slurp "/Users/chris/code/thebrigade/spotify-sifter/sifter2/data/keys.txt"))

(str/split-lines sifter-keys)

