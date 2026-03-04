## GTB Solver (Fabric 1.21.11)

Client-side Hypixel **Guess The Build** solver for **Minecraft Fabric 1.21.11**.

### What it does

- Watches the **action bar hint** (underscores + revealed letters)
- Finds matching words from word bank `wordlist.js`
- Prints **clickable guesses** into chat
  - Clicking a word runs a client command that sends that word as a chat message (your client sends the guess)

### Commands

- **`/gtbs hints`**: print candidates for the latest seen hint again (using the last action-bar pattern)
- **`/gtbs toggle`**: toggle automatic chat output of candidates whenever the hint changes
- **`/gtbs debug`**: debug tool to print the last captured action-bar overlay
- **`/gtbs guess <word>`**: manually send a guess (same as clicking one of the suggested words)

### Build

Once the Gradle wrapper is in place, build with:

```bash
./gradlew build
```

The mod jar will be in `build/libs/`.
