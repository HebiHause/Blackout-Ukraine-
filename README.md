# Blackout Ukraine

Telegram bot that shows the power outage schedule for your queue in Kyiv, counts down to the next
outage, and lists nearby Invincibility Points (Пункти Незламності). Built for a mini-grant, MVP due
2026-11-14.

- What we build and why: [docs/SPEC.md](docs/SPEC.md)
- Contribution rules: [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md)
- Decisions and the research behind them: [docs/decisions/](docs/decisions/README.md), [docs/research/](docs/research/README.md)

## Stack

Java 25 (Temurin), Maven, TelegramBots, Jackson, SQLite via JDBC, JUnit 5.

## Running locally

The project skeleton lands in the first task (week of 2026-10-19). Until then there is nothing to run.

The bot token is read from the `BOT_TOKEN` environment variable. Copy `.env.example` to `.env` and
fill it in; `.env` is git-ignored. Use the separate test bot for development, never the production one.
