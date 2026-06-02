# Contributing

## Building

```bash
mvn package
```

The compiled JAR is written to `target/lancha-<version>.jar`.

## Local Test Server

Set up a Paper test server for rapid iteration:

```bash
# 1. Download Paper 1.21.4
curl -o paper-1.21.4.jar https://api.papermc.io/v2/projects/paper/versions/1.21.4/builds/67/downloads/paper-1.21.4-67.jar

# 2. Bootstrap the server (accept EULA, let it generate files, then stop)
java -jar paper-1.21.4.jar --nogui
echo "eula=true" > eula.txt

# 3. Symlink or copy the plugin jar
cp target/lancha-*.jar plugins/
```

### Live Reload

After rebuilding, replace the jar and restart the server or use `/reload confirm`:

```bash
mvn package && cp target/lancha-*.jar /path/to/server/plugins/
```

> **Note**: Server operators in testing may use the `lancha` directory permission to
> give themselves the ability to fly, teleport, and rapidly iterate on plugin changes.

## Project Structure

```
src/main/java/com/syntask/lancha/
  Lancha.java           – Plugin entry point, recipe registration
  BoatListener.java     – Input tracking, boat tagging, speed application
  BoatSpeedManager.java – Per-player speed simulation, config
  SpeedBoatItem.java    – Speed boat item creation and HP readout
src/main/resources/
  plugin.yml            – Plugin metadata
  config.yml            – Server operator configuration
```
