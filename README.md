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

### Fly.io

Deploy to Fly.io with scale-to-zero (uses webhook mode):

```bash
# Create app and volume
fly apps create elsewhenbot
fly volumes create data --region lhr --size 1

# Set secrets
fly secrets set BOT_TOKEN=<your-bot-token>
fly secrets set WEBHOOK_URL=https://elsewhenbot.fly.dev/
fly secrets set WEBHOOK_SECRET=$(openssl rand -hex 32)

# Deploy
fly deploy
```

#### Importing data from an old instance

If you have existing data to import:

```bash
# First deploy to create the machine
fly deploy

# Copy your data file to the volume
fly sftp shell
> put /path/to/local/data.edn /data/data.edn

# Or use ssh
fly ssh console
cat > /data/data.edn << 'EOF'
<paste your data here>
EOF
```

### Docker

A docker container is provided at `ghcr.io/akeboshiwind/elsewhenbot:latest`.

### Environment Variables

| Name | Description | Default Value |
| --- | --- | --- |
| `BOT_TOKEN`* | The bot token. If you don't have one, go see the [botfather](https://telegram.me/BotFather). | N/A |
| `DATA_PATH` | The path that config is stored at. | `/data/data.edn` |
| `WEBHOOK_URL` | If set, runs in webhook mode at this URL. | N/A (polling mode) |
| `WEBHOOK_SECRET` | Secret token for webhook verification. | N/A |
| `PORT` | Port for webhook server. | `8080` |

\* Required




## Development

Use the following to start a repl

```bash
clj -M:dev
```
