package com.st0nefish.discord.openai.utils

/**
 * Usage tracker class to keep track of who is using the API, the number of text completion tokens they use, and the
 * number of images they generate. used to prevent any one user from going crazy and for general stats
 *
 * @constructor Create empty Usage tracker
 */
class UsageUtils private constructor() {
    companion object {
        private const val TEXT_TOKEN_COST = 0.002 / 1000
        private const val IMG_1024_COST = 0.02 / 1
        private const val IMG_512_COST = 0.018 / 1
        private const val IMG_256_COST = 0.016 / 1

        /**
         * get the cost of a chat request
         *
         * @param tokens number of tokens used by the request
         * @return cost of the request
         */
        fun getChatCost(tokens: Int): Double {
            return tokens * TEXT_TOKEN_COST
        }

        /**
         * get the cost of an image request
         *
         * @param size size of the generated image
         * @return cost of the request
         */
        fun getImageCost(size: String): Double {
            return when (size) {
                "256x256" -> IMG_256_COST
                "512x512" -> IMG_512_COST
                "1024x1024" -> IMG_1024_COST
                else -> IMG_1024_COST
            }
        }
    }
}