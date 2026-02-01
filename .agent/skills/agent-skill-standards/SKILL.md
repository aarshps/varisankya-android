---
name: Agent Skill Standards
description: Guidelines for creating and maintaining granular, high-quality agent skills.
---

# Agent Skill Standards

To maintain a high-quality codebase and a consistent AI pair-programming experience, skills must follow these standards.

## Granularity & Focus

1. **One Topic per Skill:** Avoid "General UI" or "Common Utils". Use specific titles like `Hero Section Stability` or `Currency Display Standards`.
2. **Minimal Duplication:** If a rule applies to multiple components (e.g., currency formatting in charts and lists), create a single central skill and reference it.
3. **Actionable Rules:** Skills must provide concrete "MUST" and "MUST NOT" rules, not just general descriptions.

## Structure

Every `SKILL.md` MUST follow this structure:
- **YAML Frontmatter:** `name` and `description`.
- **Primary Header:** Title of the skill.
- **Rules/Principles:** The core technical or design requirements.
- **Implementation Examples:** Snippets of Kotlin, XML, or Gradle code demonstrating the "Correct" way.
- **Checklist:** A short list of verification items to ensure compliance.

## Evolution

1. **Context Sync:** After completing a major feature or resolving a subtle bug (like the Dec-Oct sorting issue), immediately extract that knowledge into a skill or update an existing one.
2. **Reversion Documentation:** If a pattern is tried and reverted (like the M3E consolidated settings cards), update the architecture skill to document WHY the traditional pattern was restored.

## Metadata Guidelines
- Use clear, lowercase-hyphenated directory names.
- Keep descriptions concise for quick indexing by the AI agent.
