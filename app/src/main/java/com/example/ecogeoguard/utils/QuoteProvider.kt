package com.example.ecogeoguard.utils

import kotlin.random.Random

object QuoteProvider {

    data class Quote(val text: String, val author: String)

    private val quotes = listOf(
        Quote("The best time to plant a tree was 20 years ago. The second best time is now.", "Chinese Proverb"),
        Quote("Earth provides enough to satisfy every man's needs, but not every man's greed.", "Mahatma Gandhi"),
        Quote("We do not inherit the earth from our ancestors; we borrow it from our children.", "Native American Proverb"),
        Quote("The environment is where we all meet; where we all have a mutual interest.", "Lady Bird Johnson"),
        Quote("Technology is best when it brings people together.", "Matt Mullenweg"),
        Quote("Agriculture is our wisest pursuit, because it will in the end contribute most to real wealth, good morals, and happiness.", "Thomas Jefferson"),
        Quote("Safety is not an intellectual exercise to keep us in work. It is a matter of life and death.", "Paul O'Neill"),
        Quote("The only way forward, if we are going to improve the quality of the environment, is to get everybody involved.", "Richard Rogers"),
        Quote("Smart farming is not about doing more with less, it's about doing better with what we have.", "Modern Farmer"),
        Quote("Protecting our planet starts with protecting our communities.", "EcoGeoGuard Team")
    )

    fun getRandomQuote(): Quote {
        return quotes[Random.nextInt(quotes.size)]
    }

    fun getAllQuotes(): List<Quote> {
        return quotes
    }

    fun getQuoteForRole(role: String): Quote {
        return when(role.uppercase()) {
            "FARMER" -> quotes[5]  // Agriculture quote
            "AUTHORITY" -> quotes[6] // Safety quote
            "GOVERNMENT" -> quotes[7] // Environment quote
            else -> getRandomQuote()
        }
    }
}