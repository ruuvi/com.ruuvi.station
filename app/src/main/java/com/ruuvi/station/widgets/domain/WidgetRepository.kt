package com.ruuvi.station.widgets.domain

import kotlin.random.Random
import kotlin.random.nextInt

class WidgetRepository {
    data class Data(val name: String)

    var data:List<Data> = emptyList()

    fun updateData() {
        data = names
            .shuffled()
            .take(Random.nextInt(5..10))
            .map { Data(it) }
    }
}

private val names = listOf(
    "Sylvia",
    "Brody",
    "Cindy",
    "Jamari",
    "Maritza",
    "Vivienne",
    "Nixon",
    "Vivian",
    "Arav",
    "Cristiano",
    "Marques",
    "Nyla",
    "Darien",
    "Randall",
    "Darwin",
    "Rochel",
    "Denise",
    "Millie",
    "Journey",
    "Elora",
    "Justice",
    "Jayden",
    "Kailee",
    "Elliott",
    "Ivy",
    "Jakob",
    "Solomon",
    "Kalen",
    "Anita",
    "Peyton",
    "Hallie",
    "Kacie",
    "Beverly",
    "Janice",
    "Avianna",
    "Gian",
    "Ada",
    "Wilmer",
    "Noam",
    "Joel",
    "Harley",
    "Samantha",
    "Adolfo",
    "Lainey",
    "Carina",
    "Zavion",
    "Darrius",
    "Greysen",
    "Noel",
    "Juliet"
)