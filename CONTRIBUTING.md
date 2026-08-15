# Contributing

## Building

```bash
mvn package
```

The compiled JAR is written to `target/lancha-<version>.jar`.

## Local Test Server

Set up a Paper test server inside the `test/` directory (already gitignored):

```bash
# 1. Create the test server directory
mkdir -p test && cd test

# 2. Download Paper 26.2 (get the latest build URL from https://papermc.io/downloads/paper)
curl -L -o paper-26.2.jar "https://fill-data.papermc.io/v1/objects/bd3a58cf96874e5ea6643f5f6fe9b4f5bf9e34b795fa078c2f0ee8b98b2f907e/paper-26.2-112.jar"

# 3. Bootstrap the server (accept EULA, let it generate files, then stop)
java -jar paper-26.2.jar --nogui
echo "eula=true" > eula.txt

# 4. Copy the plugin jar
mkdir -p plugins
cp ../target/lancha-*.jar plugins/
```

### Live Reload

After rebuilding, replace the jar and restart the server or use `/reload confirm`:

```bash
mvn package && cp target/lancha-*.jar test/plugins/
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
