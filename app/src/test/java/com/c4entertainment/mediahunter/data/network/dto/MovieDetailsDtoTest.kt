package com.c4entertainment.mediahunter.data.network.dto

import com.c4entertainment.mediahunter.data.network.dto.movie_details.MovieDetailsDto
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieDetailsDtoTest {

    private val json = """
        {
          "id": 550,
          "backdrop_path": "/backdrop.jpg",
          "belongs_to_collection": { "id": 1, "name": "Collection", "poster_path": "/p.jpg" },
          "release_date": "1999-10-15",
          "vote_average": 8.4,
          "spoken_languages": [ { "english_name": "English", "iso_639_1": "en", "name": "English" } ],
          "videos": { "results": [ { "key": "abc123", "site": "YouTube", "published_at": "2016-02-16" } ] },
          "credits": { "cast": [ { "id": 819, "name": "Edward Norton", "profile_path": "/en.jpg" } ] }
        }
    """.trimIndent()

    @Test
    fun `deserializes snake_case fields`() {
        val dto = Moshi.Builder().build().adapter(MovieDetailsDto::class.java).fromJson(json)!!

        assertEquals(550, dto.id)
        assertEquals("/backdrop.jpg", dto.backdropPath)
        assertEquals("/p.jpg", dto.belongsToCollection?.posterPath)
        assertEquals("1999-10-15", dto.releaseDate)
        assertEquals(8.4, dto.voteAverage!!, 0.0)
        assertEquals("English", dto.spokenLanguages.first().englishName)
        assertEquals("en", dto.spokenLanguages.first().iso6391)
        assertEquals("abc123", dto.videos?.results?.first()?.key)
        assertEquals("2016-02-16", dto.videos?.results?.first()?.publishedAt)
        assertEquals("Edward Norton", dto.credits?.cast?.first()?.name)
    }
}
