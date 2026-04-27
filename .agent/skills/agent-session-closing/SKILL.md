---
name: Agent Session Closing
description: Standard operating procedure for concluding an AI agent session and ensuring workspace integrity.
---

# Agent Session Closing Protocol

Before concluding a development session, the agent MUST execute the following steps to ensure the workspace is left in a clean, documented, and resilient state for future agents or human developers.

## 1. Documentation & Guardrails Update
- Review the tasks accomplished during the session.
- Update `AGENTS.md`, `README.md`, and any relevant `SKILL.md` files to reflect new architectural decisions, environment changes, or newly discovered constraints.
- If a new repeated task was identified, create a new granular skill (under 100 lines) in `.agent/skills/`.

## 2. Workspace Cleanup
- Ensure no sensitive credentials, keystores, or `.env` variables were accidentally staged or committed.
- Verify that `retrieve_secrets.sh` (or similar environment setup scripts) are documented and ignored in `.gitignore` if they contain sensitive data.
- Remove any temporary debugging files or local test artifacts not meant for version control.

## 3. Final Validation
- Run the core build command (e.g., `./gradlew assembleDebug` for Android) to ensure the project compiles successfully in its final state.
- Run any available linting or formatting checks.

## 4. Commit and Push
- Stage all documented changes and finalized code.
- Create a clear, concise commit message summarizing the session's impact.
- Push the changes to the remote repository (e.g., `git push origin main`).

## 5. Session Summary
- Provide the user with a concise summary of what was accomplished, what documentation was updated, and any recommended next steps for the following session.
