# Discord-OpenAI

A basic Discord bot for interfacing with OpenAI's ChatGPT and Dall-E tools built using the
[Kord discord bot framework](https://github.com/kordlib/kord) and
the [openai-kotlin client](https://github.com/Aallam/openai-kotlin).

## Hosting

This project includes all the code, but requires the user to provide their own Discord and OpenAI auth tokens. This
allows the bot host full control over the bot and OpenAI billing.

### Pre-Requisites

1. Acquire a Discord bot token
    * https://github.com/kordlib/kord/wiki/Getting-Started#starting-your-bot-with-kord
2. Acquire an OpenAI auth token
    * https://platform.openai.com/docs/api-reference/authentication
3. Get your Discord user ID - this needs to be the Snowflake value
    * https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-

### Docker

The suggested way to host this project is with Docker compose. This makes everything much easier and avoids the need to
manage dependencies and JVM install.

Create a file called `docker-compose.yml` and add the following contents. Replace the values wrapped in `{}` with the
ones retrieved in the Pre-Requisites section above. These are all *required* for the bot to function as expected.

This example also maps the persistent host directory `./config` in the same folder as the compose file to the container.
This will contain persistent data, including the full configuration file, and the database with usage data.

#### Compose File

```
services:
  discord-openai:
    image: st0nefish/discord-openai
    container_name: discord-openai
    volumes:
      - "./config:/app/discord-openai/config:rw"
    environment:
      - "DISCORD_GPT_BOT_TOKEN=<DISCORD_BOT_TOKEN>"     # required
      - "DISCORD_GPT_OPENAI_TOKEN=<OPENAI_AUTH_TOKEN>"  # required
      - "DISCORD_GPT_ADMIN_GUILDS=<ADMIN_GUILD_ID_CSV>" # optional
      - "DISCORD_GPT_ADMIN_USERS=<ADMIN_USER_ID_CSV>"   # optional
  healthcheck:
    disable: true
  security_opt:
    - no-new-privileges:true
  restart: unless-stopped
```

## Config

There are several ways to configure this bot

### Environment Variables

There are a handful of environment variables that can be set to configure the bot. A few of these are always required,
the rest will be read only on first usage after which they will be stored as part of the configuration file, stored by
default in `./config/config.json`, though this can be configured with the `DISCORD_GPT_BOT_PATH_CONFIG` environment
variable. Once this config file is generated it will be used in place of the environment variables.

#### Required Variables

These are consumed on every boot and the bot will fail to start if they are not set or set to invalid values. They will
not be stored to the config file

| Variable                 | Purpose                                  |
|:-------------------------|:-----------------------------------------|
| DISCORD_GPT_BOT_TOKEN    | Discord authentication token for the bot |
| DISCORD_GPT_OPENAI_TOKEN | OpenAI authentication token              |

#### Optional Variables

These settings are consumed on boot, but are optional and have functional default values.

| Variable                 | Default                | Purpose                                                                                                                                             |
|:-------------------------|:-----------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------|
| DISCORD_GPT_PATH_CONFIG  | `./config/config.json` | Path to the configuration file                                                                                                                      |
| DISCORD_GPT_PATH_DB      | `./config/data.db`     | Path to the database file                                                                                                                           |
| DISCORD_GPT_ADMIN_GUILDS |                        | Comma-separated list of Snowflake IDs for guilds in which to register admin commands. If none are specified then the admin commands cannot be used. |
| DISCORD_GPT_ADMIN_USERS  |                        | Comma-separated list of Snowflake IDs for users with admin permissions. If none are specified then no users will be able to use the admin commands. |

#### Default Values

These are consumed only on first boot and used to default values in the config file. Once the config file has been
created the environment variables will no longer be used, though most of the settings can also be managed via the admin
commands registered in the configured admin guild(s).

| Variable                       | Purpose                                                                                                                                                           |
|:-------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DISCORD_GPT_ALLOW_CHANNELS     | Comma-separated list of Snowflake IDs for channels the bot is allowed to operate within. If left empty then the bot will operate in all channels it is present in |
| DISCORD_GPT_ALLOW_USERS        | Comma-separated list of Snowflake IDs for users the bot is allowed to PM with. If left empty all users can PM with this bot                                       |
| DISCORD_GPT_COST_MAX_USER      | Maximum usage cost (in dollars) for each user per configured time period. Defaults to $0.50                                                                       |
| DISCORD_GPT_COST_TIME_INTERVAL | Time interval (in hours) at which the the usage tracker resets. Defaults to 24 hours                                                                              |
| DISCORD_GPT_UNLIMITED_USERS    | Comma-separated list of Snowflake IDs for users exempt from the daily usage cap.                                                                                  |
| DISCORD_GPT_CLEAN_START        | If set to 'true' then the bot will de-register all commands on boot. Useful mostly for dev purposes or if some commands are no longer supported.                  |

### Config File

A JSON file containing the serialized values of will be generated on the filesystem. All settings within this file can
be managed via commands in the admin guild(s) (if configured). Changes to this file will not be recognized until after
rebooting the bot, and any changes made via Discord commands in the admin guild(s) will overwrite changes made without a
reboot. This file's location can be set with the `DISCORD_GPT_PATH_CONFIG` environment variable, and will default to
`./config/config.json`

## Usage

The bot will register a few commands in all guilds it is present within, along with a handful of admin/management
commands only in the guilds configured as admin guilds.

For all the below commands that support it, the optional `public` argument determines if the response should be only to
the user who executed the command (argument left out or public=false) or output to the channel (public=true).

All arguments in `[braces]` are optional.

### General Commands

These commands are registered in all guilds that the bot is present within and cover the basic "communicate with OpenAI"
use cases.

| Command         | Arguments        | Description                                                           |
|:----------------|:-----------------|:----------------------------------------------------------------------|
| ask-gpt         | prompt           | send a prompt to Chat GPT                                             |
| ask-dalle       | size, prompt     | send a prompt to Dall E to generate an image of the specified size    |
| get-usage       | [user], [public] | get the current usage stats for the specified user (defaults to self) |
| get-usage-total | [public]         | get the total current usage stats - limited to admin users            |
| info            | -                | get some basic info for this bot                                      |
| help            | [command]        | get help for the bot, or if a command is specified, for that command  |
| ping            | -                | check if the bot is actually working                                  |

### Admin Commands

These commands are registered only in guilds specified as admin guilds to avoid adding unnecessarily to the list of
slash commands. These are generally used to manage the bot.

| Command              | Arguments   | Description                                                   |
|:---------------------|:------------|:--------------------------------------------------------------|
| get-config           | -           | output the current configuration                              |
| add-admin-user       | user        | add an admin user                                             |
| show-admin-users     | -           | show the current list of admin users                          |
| add-allow-channel    | channel     | add a channel to the list of allowed channels                 |
| show-allow-channels  | -           | show the current list of allowed channels                     |
| add-allow-user       | user        | add a user to the list of those allowed to use the bot via PM |
| show-allow-users     | -           | show the current list of users allowed to use the bot via PM  |
| add-unlimited-user   | user        | add a user to the list of those exempt from usage limits      |
| show-unlimited-users | -           | show the current list of unlimited users                      |
| get-last-chat        | user        | get the last chat exchange for the specified user             |
| get-last-image       | user        | get the last image exchange for the specified user            |
| set-usage-limit      | value, cost | set the new usage limit to $value/$interval hours/user        |