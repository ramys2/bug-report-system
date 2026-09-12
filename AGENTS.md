# AGENTS.md

## Project Overview

The goal of this project is to create a minimalistic clone of Jira. The project is intended primarily as a school submission and is unlikely to be maintained long-term. However, it should still be well-structured, understandable, and presentable on GitHub.

When making changes, prioritize maintainability and simplicity. Code should remain easy to understand and maintain.

## Project Structure

- `frontend/src/`
  - `components/` - Contains reusable UI components.
  - `api/` - Contains JavaScript modules for sending REST API requests.
  - `pages/` - Contains application pages composed of components from `components/`.

- `backend/`
  - `bug-report-domain/` - Contains domain classes and domain-related logic.
  - `bug-report-api/` - Contains the Spring Boot application and API-related configuration.

## Technologies

### Frontend

- React
- Bootstrap
- Vite

### Backend

- Java
- Spring Boot
- Maven

### Infrastructure

- Docker
- Database - not yet implemented

## Learning and Explanations

I am a junior developer and I want to understand the code I work with, not just produce working solutions.

When working on tasks:

- Before implementing a non-trivial solution, briefly explain the proposed approach and why it fits the existing codebase.
- When introducing a concept, API, library feature, framework pattern, or language feature that may not be obvious, explain what it does and why it is being used.
- Prefer explanations based on the actual codebase rather than generic tutorials.
- Point out important data flow, control flow, lifecycle behavior, side effects, and interactions between components when relevant.
- When there are multiple reasonable approaches, briefly explain the main tradeoff and why you chose one.
- Do not over-explain basic syntax or concepts that are already clearly used throughout the codebase.
- Keep explanations practical and concise. Focus on knowledge needed to understand, review, debug, and maintain the implementation.
- Do not hide complexity behind unnecessary abstractions. Prefer code that is straightforward for a junior developer to follow.
- If I ask for help rather than explicitly asking for an implementation, prefer guiding me with explanations and hints before providing the complete solution.

## Project-specific Guidance

- Use `camelCase` for JSON fields in API request and response DTOs.

## Engineering Guidance

### Development workflow

Work incrementally and keep changes small and reviewable.

For non-trivial tasks:

1. Inspect the relevant code before making changes.
2. Propose an implementation approach and define the scope of the **next iteration only**.
3. Do not modify files until I explicitly approve that iteration.
4. After approval, implement **only the scope approved for that iteration**.
5. Summarize what was changed, any decisions made, and what I should review.
6. **Stop after completing the approved iteration. Do not begin, propose, or implement the next iteration until I explicitly tell you to continue.**
7. My approval applies only to the iteration currently being discussed. **Never treat approval of one iteration or of the overall approach as permission to complete subsequent iterations.**

Do not:

- Make unrelated changes.
- Refactor code outside the approved scope.
- Expand the scope autonomously.
- Batch multiple implementation iterations together.
- Continue implementing because the next step seems obvious.
- Interpret approval of a plan as approval to implement the entire plan.

If you discover during implementation that the approved approach or scope needs to change, stop and explain why. Wait for explicit approval before proceeding with the changed approach.

### Principles

- Prefer the simplest correct implementation. Avoid speculative features, abstractions, dependencies, configuration, and dead code.
- Make narrowly scoped changes that preserve existing behavior unless the task explicitly requires otherwise.
- Follow the coding conventions and patterns of the surrounding code.
- Do not refactor unrelated code while implementing a change.
- Prefer existing project patterns and utilities over introducing new ones.
- Write robust, production-safe code: validate untrusted input, apply least privilege, protect secrets, use safe defaults, and fail closed when authorization or security checks are uncertain.
- Do not weaken security controls, bypass verification, log credentials or sensitive data, or introduce unsafe shortcuts to make a task pass.
- Use established project patterns and dependencies. Add a dependency only when necessary and after verifying it is reputable, maintained, and appropriate.
- Verify claims and implementation decisions with the codebase, official documentation, specifications, or other authoritative sources. Do not guess.
- Before completion, run the smallest relevant validation (tests, typecheck, lint, build) if allowed to and report any limitation or unresolved risk plainly.
- Before running commands, always start with a dummy variable named `$explanation` that explains what you are going to do and why in as few words as possible. This allows the user to see what will be done before the command is accepted and executed.

### Communication

- Keep responses concise while retaining material facts, decisions, risks, and verification results.
- Lead with the conclusion, then provide just enough rationale to convey the intuition behind it.
- Distinguish verified facts from assumptions and recommendations.
