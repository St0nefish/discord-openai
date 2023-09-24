# start from corretto 17 image
FROM @docker_base_image@

# set author label
LABEL authors="st0nefish" \
      version="@project_version@" \
      description="Discord bot to utilize OpenAI"

# create app directory
RUN mkdir /app

# add our distribution and move to static directory
ADD discord-openai-@project_version@.tar /app
RUN mv /app/discord-openai-@project_version@ /app/discord-openai

# add logger config
ADD simplelogger.properties /app/discord-openai

# set working directory and execute
WORKDIR /app/discord-openai
CMD ["bin/discord-openai"]