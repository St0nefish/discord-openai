# start from configured base image
FROM @docker_base_image@

# set labels
LABEL authors="st0nefish" \
      version="@project_version@" \
      description="Discord bot to utilize OpenAI"

# extract our latest distribution to /tmp
ADD discord-openai-@project_version@.tar /tmp

# move extracted distribution to /app for easier usage
RUN mv /tmp/discord-openai-@project_version@ /app

# add logger config file
ADD simplelogger.properties /app

# set working directory and execute
WORKDIR /app
CMD ["bin/discord-openai"]