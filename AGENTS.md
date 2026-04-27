# AI Agents Context

This repository utilizes AI agents (such as Gemini CLI, Cline, etc.) for development, maintenance, and orchestration.

## Core Agent Mandates

1. **Security First:** Agents must NEVER log, print, stage, or commit secrets, API keys, keystores, or sensitive credentials. All deployment secrets and API keys for this project are securely managed via Bitwarden. 
   - **Important:** The project now builds on a headless Ubuntu Linux machine.
   - Run `./retrieve_secrets.sh` to extract the keystore and Firebase config securely from Bitwarden CLI into the local workspace (`app/google-services.json`, `varisankya-upload-key`, and `local.properties`).
2. **Context Efficiency:** Agents should minimize unnecessary context usage by targeting file reads and searches efficiently.
3. **Engineering Standards:** Adhere strictly to existing workspace conventions, architectural patterns, and styling. Never bypass type systems or suppress warnings without explicit user instruction.
4. **Validation:** Agents must empirically validate all changes (e.g., compile checks with `./gradlew assembleDebug`, linting, tests) before considering a task complete.

## Operational Workflows
* **Execution:** Operate in a Plan -> Act -> Validate cycle.
* **Session Closing:** Before closing a session, agents MUST follow the `Agent Session Closing Protocol` (see `.agent/skills/agent-session-closing/SKILL.md`) to update documentation, clean up, and push changes to the repository.
* **Tool Usage:** Prefer specific tools (e.g., targeted file replacement) over rewriting entire files. Run commands non-interactively where possible.
* **Headless Build Environment:** The project is configured for Linux CLI builds without Android Studio. `ANDROID_HOME` is set to `~/Android/Sdk`.
* **Releases:** See `CLI_RELEASE_GUIDE.md` for building and distributing updates via GitHub or Google Play Console.
