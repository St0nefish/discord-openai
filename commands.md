# Commands

## Key 
| Symbol      | Meaning                        |
|-------------|--------------------------------|
| [Argument]  | Argument is not required.      |

## ChatGPT
| Commands | Arguments | Description             |
|----------|-----------|-------------------------|
| ask-gpt  | Prompt    | ask chat GPT a question |

## DallE
| Commands  | Arguments          | Description                   |
|-----------|--------------------|-------------------------------|
| ask-dalle | image-size, Prompt | generate an image with DALL·E |

## admin
| Commands                 | Arguments        | Description                                                 |
|--------------------------|------------------|-------------------------------------------------------------|
| admin-add-channel        | ChannelID        | add a channel to the allow list                             |
| admin-add-unlimited-user | UserID           | add a user to the unlimited users list                      |
| admin-get-config         |                  | admin command to get bot configuration                      |
| admin-get-last-chat      | [user], [public] | get the last chat exchange for a user                       |
| admin-get-last-image     | [user], [public] | get the last image exchange for a user                      |
| admin-show-channels      |                  | list channels this bot is allowed to respond to commands in |
| admin-show-users         |                  | list users in the unlimited use list                        |
| admin-total-usage        | [public]         | get total OpenAI API usage stats                            |
| admin-user-usage         | [user], [public] | get user OpenAPI stats                                      |
| context-admin-user-usage | User             | get OpenAPI usage stats for this user                       |

## usage
| Commands        | Arguments | Description                              |
|-----------------|-----------|------------------------------------------|
| get-usage-stats |           | get your personal OpenAI API usage stats |

## utility
| Commands | Arguments | Description           |
|----------|-----------|-----------------------|
| help     | [Command] | Display a help menu.  |
| info     |           | Bot info for Ai-thena |

