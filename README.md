## GTB Solver (Fabric 1.21.11)

Client-side Hypixel **Guess The Build** solver for **Minecraft Fabric 1.21.11**.

### What it does

- Watches the **action bar hint** (underscores + revealed letters)
- Finds matching words from word bank `wordlist.js`
- Prints **clickable guesses** into chat
  - Clicking a word runs a client command that sends that word as a chat message (your client sends the guess)

### Commands

- **`/gtbsolve`**: print candidates for the latest seen hint again
- **`/gtbsolve toggle`**: toggle auto-output on hint change
- **`/gtbsolve debug`**: debug tool to print action bar

### Build

Once the Gradle wrapper is in place, build with:

```bash
./gradlew build
```

The mod jar will be in `build/libs/`.
