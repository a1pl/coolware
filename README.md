opencoolware

shitty vibecoded hypixel client made by cooldood
was being sold for 15 dollars

telly scaffold doesnt even jump or turn :sob:

## Building

needs a java 8 and maven, built on mavenmcp 1.8.9

```
mvn clean package
```

the client jar ends up at `target/MCP-1.8.9-jar-with-dependencies.jar`. copy it to `versions/Coolware/Coolware.jar` (with `Coolware.json` next to it) to use it from a launcher

## Running

- **Linux:** `./run-linux.sh` compiles the source and starts the client. It needs a built `target/MCP-1.8.9-jar-with-dependencies.jar` for the libraries, and java 8 on `PATH`
- **Windows:** `launcher.bat`
- **IDE:** run `Start.java` with the working directory set to `./test_run/`

the game directory is `test_run/`. saves, configs and alt accounts are stored there 
