# AeroAI

A client-side NeoForge mod for Minecraft 1.21.1 that adds a local AI chatbot through [Ollama](https://ollama.com).

I made this for the AeroSMP but this can be used anywhere.

When anyone in chat says the wake word, AeroAI sends everything after it to an Ollama model and posts the model's reply to chat from your account:

```
<Steve> aero what's the best y-level for diamonds?
<You> [aero] [gemma4:latest] : Around Y -58, just above the deepslate bedrock layers.
```

## Features

- **Wake word trigger:** the word must stand on its own, so `aero` won't fire on `AeroPilot`.
- **Works on any server:** AeroAI is client-side only, so the server doesn't need it installed. It reads both player chat and server messages, which covers servers that relay chat through plugins.
- **In-game config screen:** Mods → AeroAI → Config. Text fields have no length limit, so long system prompts fit.
- **Conversation memory:** the model remembers recent questions and answers.
- **Chat-safe output:** markdown and line breaks are removed, and long replies are split across several messages with a delay between them.
- **Loop and spam protection:** it ignores its own replies, has a cooldown, and handles one request at a time.
- **`/aeroai` commands** for quick changes without leaving the game.

## Requirements

- Minecraft **1.21.1** with **NeoForge 21.1.x**
- [Ollama](https://ollama.com) running somewhere your PC can reach, by default `http://localhost:11434`, with at least one model pulled:
  ```bash
  ollama pull gemma4
  ```

## Installation

1. Download the jar from [Releases](https://github.com/HydrogenRocket/Aero-AI/releases), or [build it yourself](#building).
2. Put it in your `.minecraft/mods` folder.
3. Start the game, then open **Mods → AeroAI → Config** and pick your model.

## Usage

Type the wake word in chat, followed by your question:

```
aero how do I make a beacon?
```

Anyone else in chat can trigger it too. The reply is sent from your account.

### Commands

| Command | Description |
|---|---|
| `/aeroai` | Show current status and settings |
| `/aeroai on` / `off` | Turn the bot on or off |
| `/aeroai config` | Open the config screen |
| `/aeroai models` | List the models installed in Ollama |
| `/aeroai model <name>` | Switch model (tab-completes) |
| `/aeroai wakeword <word>` | Change the wake word |
| `/aeroai system <prompt>` | Set the system prompt (up to 256 characters; use the config screen for longer prompts) |
| `/aeroai ask <question>` | Ask the model and post the reply to chat |
| `/aeroai test <question>` | Ask the model and show the reply only to you |
| `/aeroai clear` | Clear the conversation memory |

## Configuration

Change these in-game, or edit `config/aeroai-client.toml` directly.

| Setting | Default | Description |
|---|---|---|
| **General** | | |
| `enabled` | `true` | Master on/off switch |
| `wakeWord` | `aero` | Word that triggers the bot |
| `caseSensitive` | `false` | Whether the wake word must match case exactly |
| `respondToSelf` | `true` | Let your own messages trigger the bot |
| `respondToSystemMessages` | `true` | Also scan server/system messages |
| `includeSenderName` | `true` | Tell the model who asked |
| `cooldownSeconds` | `3` | Minimum time between requests |
| **Ollama** | | |
| `ollamaUrl` | `http://localhost:11434` | Ollama server address |
| `model` | `gemma4:latest` | Model to use |
| `displayName` | *(blank)* | Name shown in chat instead of the model name |
| `systemPrompt` | *short, plain-text replies* | System prompt sent to the model |
| `temperature` | `0.7` | Sampling temperature |
| `maxTokens` | `200` | Maximum length of a reply, in tokens |
| `disableThinking` | `true` | Skip the reasoning phase on thinking models for faster replies |
| `historySize` | `4` | Number of past question/answer pairs to remember (0 turns memory off) |
| `timeoutSeconds` | `120` | How long to wait for a reply |
| **Output** | | |
| `messageFormat` | `[{wakeword}] [{model}] : {reply}` | Chat format; placeholders are `{wakeword}`, `{model}` and `{reply}` |
| `maxMessages` | `3` | Maximum chat messages per reply |
| `messageDelayMs` | `1200` | Delay between split messages |

## Building

Requires JDK 21.

```bash
git clone https://github.com/HydrogenRocket/Aero-AI.git
cd Aero-AI
./gradlew build
```

The jar is written to `build/libs/`. Run `./gradlew runClient` to launch a development client with the mod loaded.

## Notes

- Replies are sent from **your account**, so you're responsible for what the bot says. Check the server's rules on bots and automated chat first.
- If the server kicks you for spam, raise `messageDelayMs` or `cooldownSeconds`, or lower `maxMessages`.
- To use Ollama on another machine, set `ollamaUrl` to its address. You may need to start Ollama with `OLLAMA_HOST=0.0.0.0` so it accepts outside connections.
