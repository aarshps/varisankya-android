# AI Agents Context

This repository utilizes AI agents (such as Gemini CLI, Cline, etc.) for development, maintenance, and orchestration.

## Core Agent Mandates

1. **Security First:** Agents must NEVER log, print, stage, or commit secrets, API keys, keystores, or sensitive credentials. All deployment secrets and API keys for this project are securely managed via Bitwarden. Refer to the local `PUBLISHING_SECRETS_RECOVERY.md` or `SECRETS_RECOVERY.md` files for retrieval instructions.
2. **Context Efficiency:** Agents should minimize unnecessary context usage by targeting file reads and searches efficiently.
3. **Engineering Standards:** Adhere strictly to existing workspace conventions, architectural patterns, and styling. Never bypass type systems or suppress warnings without explicit user instruction.
4. **Validation:** Agents must empirically validate all changes (e.g., compile checks, linting, tests) before considering a task complete.

## Operational Workflows
* **Execution:** Operate in a Plan -> Act -> Validate cycle.
* **Tool Usage:** Prefer specific tools (e.g., targeted file replacement) over rewriting entire files. Run commands non-interactively where possible.
