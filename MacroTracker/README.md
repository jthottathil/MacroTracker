# MacroTracker

A Java desktop calorie and macro tracker built with JavaFX and JDBC in an
MVC-style architecture (`application`, `controller`, `model`, `view`, `service`
packages), backed by an embedded SQLite database — no server to install.

## Requirements

- JDK 17+ (developed against JDK 23)
- Nothing else. No Eclipse, no MySQL server, no Maven/Gradle. `./setup.sh`
  downloads the JavaFX and SQLite JDBC jars this project needs into `lib/`
  (skipped if they're already there).

## Building and running

```
./run.sh
```

This downloads dependencies into `lib/` on first run (via `setup.sh`),
compiles into `bin/` (via `build.sh`), and launches the app. Run `./build.sh`
on its own any time you just want to recompile.

Login is hardcoded for now (`joe` / `1234` in `controller/LoginController.java`).

## The database

MacroTracker uses SQLite instead of MySQL — there's no server to install or
configure. The database file lives at `~/.macrotracker/macrotracker.db` and
is created automatically, with its schema (`food_entries`, `goals`,
`calories_burned` tables), the first time the app runs
(`model/DatabaseConnection.java`). Delete that file to reset all data.

## AI-assisted food logging

The Add Food screen can turn a plain-language description ("2 eggs, a slice
of toast, and a cup of black coffee") into structured food entries using the
Anthropic API (`service/ClaudeFoodParser.java`). To use it:

1. Get an API key from [console.anthropic.com](https://console.anthropic.com).
2. Set it as an environment variable named `ANTHROPIC_API_KEY` in the shell
   you launch the app from, e.g. `export ANTHROPIC_API_KEY=sk-ant-...` before
   `./run.sh` (never hardcode it in the source).
3. Run the app, type a description on the Add Food screen, and click
   "Parse with AI". Review/edit the parsed rows (all cells are editable),
   then "Add All to Log".

If the key is missing or invalid, or the network call fails, the screen
shows a plain-language error instead of crashing — it never silently falls
back to guessed data.
