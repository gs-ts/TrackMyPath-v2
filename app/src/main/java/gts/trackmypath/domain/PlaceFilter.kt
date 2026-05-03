package gts.trackmypath.domain

enum class PlaceFilter(
    val types: List<String>,
    val title: String
) {
    CULTURE(
        types = listOf(
            "art_gallery",
            "museum",
            "library",
            "place_of_worship"
        ),
        title = "Culture"
    ),
    ENTERTAINMENT(
        listOf(
            "amusement_center",
            "amusement_park",
            "aquarium",
            "casino",
            "movie_theater",
            "night_club",
            "zoo",
            "entertainment_complex"
        ),
        title = "Entertainment"
    ),
    FOOD_AND_DRINKS(
        listOf(
            "restaurant",
            "cafe",
            "bar",
            "bakery",
            "coffee_shop",
            "brewery",
            "distillery"
        ),
        title = "Food and drinks"
    ),
    SHOPPING(
        listOf(
            "clothing_store",
            "department_store",
            "electronics_store",
            "shopping_mall",
            "supermarket",
            "gift_shop"
        ),
        title = "Shopping"
    ),
    SPORTS(
        listOf(
            "gym",
            "stadium",
            "sports_complex",
            "swimming_pool",
            "golf_course"
        ),
        title = "Sports"
    )
}
