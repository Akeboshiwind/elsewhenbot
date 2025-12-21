# elsewhenbot

A bot to help convert between timezones in a telegram chat using natural
language. Inspired by the [Elsewhen](https://apps.apple.com/gb/app/elsewhen/id1588708173)
app.

Cooked up in a day to solve a problem :)



## Usage

1. Add [@elsewhenbot](http://t.me/elsewhenbot) to your chat
2. Use `/add@elsewhenbot <timezone identifier>` to add timezones to the output
    - Find a list of timezone ids on wikipedia [here](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones)
3. Use `/remove@elsewhenbot <timezone identifier>` to remove them
4. Use `/me@elsewhenbot <timezone identifier>` to tell the bot your timezone
5. Use `/default@elsewhenbot <timezone identifier>` to set the default for your chat
6. Message the bot `@elsewhenbot Next Tuesday at 2pm` and get the right times output



## Deployment

### Docker

A docker container is provided at `ghcr.io/akeboshiwind/elsewhenbot:latest`.

### Environment Variables

| Name | Description | Default Value |
| --- | --- | --- |
| `TELEGRAM_BOT_TOKEN`* | The bot token. If you don't have one, go see the [botfather](https://telegram.me/BotFather). | N/A |
| `DATA_PATH` | The path that config is stored at. | `/data/data/edn` |
| `HEALTH_CHECK_THRESHOLD_SECONDS` | Maximum seconds without receiving updates before the bot exits (health check). | `120` |

* - Required




## Development

Use the following to start a repl

```bash
clj -M:dev
```
