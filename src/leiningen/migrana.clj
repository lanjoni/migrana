(ns leiningen.migrana
  (:require [leiningen.core.main :as main]
            [leiningen.help :as help]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [environ.core :as environ]
            [migrana.core :as core]))

(def ^:private cli-options
  [["-s" "--schema SCHEMA_FILE" "Schema file"
    :id :schema
    :default "resources/schema.edn"]

   ["-m" "--migrations MIGRATIONS_PATH" "Migrations path"
    :id :migrations
    :default "resources/migrations/"]

   [nil "--no-inference" "Runs with no schema change inference"
    :id :no-inference
    :default false]])

(defn info
  "Shows current database information.

  Syntax: lein migrana info <URI>

  <URI> defaults to environment variable $DATOMIC_URI if available."
  [project args]
  (if-let [uri (or (second args) (environ/env :datomic-uri))]
    (core/info uri)
    (main/abort "Must specify <URI>. More details: $ lein help migrana info")))

(defn create
  "Creates new manual migration.

  Syntax: lein migrana create <name>"
  [project args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        n (second arguments)]
    (cond
      errors
      (do
        (println "Error parsing options:" (clojure.string/join "\n" errors))
        (main/abort summary))

      (nil? n)
      (main/abort "Must specify <name>. More details: $ lein help migrana name")

      :else
      (core/create n options))))

(defn dry-run
  "Simulates what `run` would do.

  Syntax: lein migrana dry-run <URI> <options>

  <URI> defaults to environment variable $DATOMIC_URI if available."
  [project args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        uri (or (second arguments) (environ/env :datomic-uri))]
    (cond
      errors
      (do
        (println "Error parsing options:" (clojure.string/join "\n" errors))
        (main/abort summary))

      (nil? uri)
      (main/abort "Must specify <URI>. More details: $ lein help migrana dry-run")

      :else
      (core/dry-run uri options))))

(defn run
  "Transacts pending migrations onto database.

  Syntax: lein migrana run <URI> <options>

  <URI> defaults to environment variable $DATOMIC_URI if available."
  [project args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        uri (or (second arguments) (environ/env :datomic-uri))]
    (cond
      errors
      (do
        (println "Error parsing options:" (clojure.string/join "\n" errors))
        (main/abort summary))

      (nil? uri)
      (main/abort "Must specify <URI>. More details: $ lein help migrana run")

      :else
      (core/run uri options))))

(defn set-db
  "Sets the database timestamp.

  Syntax: lein migrana set-db <URI> <timestamp> <options>

  <URI> defaults to environment variable $DATOMIC_URI if available."
  [project args]
  (letfn [(abort []
            (main/abort
             "Must specify <URI> and <timestamp>. More details: $ lein help migrana set-db"))]
    (if-let [uri (or (second args) (environ/env :datomic-uri))]
      (if-let [ts (get (vec args) (if (= uri (environ/env :datomic-uri)) 1 2))]
        (core/set-db uri ts)
        (abort))
      (abort))))

(defn
  ^:higher-order ^:no-project-needed
  ^{:subtasks [#'info
               #'create
               #'dry-run
               #'run
               #'set-db]}
  migrana
  "Datomic migration tool.

  Syntax: lein migrana <subtask> <options>"
  [project & args]
  (try
    (case (first args)
      "info"    (info project args)
      "create"  (create project args)
      "dry-run" (dry-run project args)
      "run"     (run project args)
      "set-db"  (set-db project args)
      nil       (help/help project "migrana"))
    (catch Throwable t
      (main/abort t))))
