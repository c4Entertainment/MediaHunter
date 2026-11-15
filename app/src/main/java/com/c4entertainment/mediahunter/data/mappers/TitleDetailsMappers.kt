package com.c4entertainment.mediahunter.data.mappers

import com.c4entertainment.mediahunter.data.network.dto.movie_details.MovieDetailsDto
import com.c4entertainment.mediahunter.data.network.dto.tv.SeasonDetailsDto
import com.c4entertainment.mediahunter.data.network.dto.tv.TvEpisodeDto
import com.c4entertainment.mediahunter.domain.model.movie_details.Cast
import com.c4entertainment.mediahunter.domain.model.movie_details.MovieVideo
import com.c4entertainment.mediahunter.domain.model.movie_details.SeasonDetails
import com.c4entertainment.mediahunter.domain.model.movie_details.TitleDetails
import com.c4entertainment.mediahunter.domain.model.movie_details.TitleSummary
import com.c4entertainment.mediahunter.domain.model.movie_details.Release
import com.c4entertainment.mediahunter.domain.model.movie_details.TvEpisode
import com.c4entertainment.mediahunter.domain.model.movie_details.TvSeason

private const val MAX_NAMES = 3
private val WRITER_JOBS = setOf("Screenplay", "Writer", "Story")

fun MovieDetailsDto.toTitleDetails(type: String): TitleDetails {
    val isTv = type == "tv"
    val crew = credits?.crew.orEmpty()
    val title = (if (isTv) name else this.title) ?: name ?: this.title ?: ""
    return TitleDetails(
        id = id,
        type = type,
        title = title,
        originalTitle = (if (isTv) originalName else originalTitle) ?: "",
        releaseDate = (if (isTv) firstAirDate else releaseDate) ?: "",
        lastAirDate = lastAirDate?.takeIf { it.isNotBlank() },
        inProduction = inProduction == true,
        status = status?.takeIf { it.isNotBlank() },
        releases = if (isTv) emptyList() else usReleases(),
        tagline = tagline?.takeIf { it.isNotBlank() },
        overview = overview?.takeIf { it.isNotBlank() },
        posterPath = posterPath?.takeIf { it.isNotBlank() },
        backdropPath = backdropPath?.takeIf { it.isNotBlank() },
        runtime = (runtime ?: episodeRunTime.firstOrNull())?.takeIf { it > 0 },
        certification = certification(isTv),
        voteAverage = voteAverage ?: 0.0,
        imdbId = (imdbId ?: externalIds?.imdbId)?.takeIf { it.isNotBlank() },
        genres = genres.mapNotNull { it.name?.takeIf(String::isNotBlank) },
        directors = if (isTv) {
            createdBy.mapNotNull { it.name }
        } else {
            crew.filter { it.job == "Director" }.mapNotNull { it.name }
        }.distinct(),
        writers = crew.filter { it.job in WRITER_JOBS }.mapNotNull { it.name }.distinct().take(MAX_NAMES),
        studios = productionCompanies.mapNotNull { it.name?.takeIf(String::isNotBlank) }.take(MAX_NAMES),
        videos = videos?.results.orEmpty()
            .filter { it.site == "YouTube" && !it.key.isNullOrBlank() }
            // Trailers first: they are what people open a title's videos for.
            .sortedBy { if (it.type == "Trailer") 0 else 1 }
            .map { MovieVideo(name = it.name, key = it.key, site = it.site, type = it.type) },
        cast = credits?.cast.orEmpty().map {
            Cast(id = it.id, name = it.name, profilePath = it.profilePath, character = it.character)
        },
        recommendations = recommendations?.results.orEmpty().map {
            TitleSummary(
                id = it.id,
                type = it.mediaType ?: type,
                title = it.title ?: it.name ?: "",
                posterPath = it.posterPath,
                year = (it.releaseDate ?: it.firstAirDate)?.take(4)?.takeIf { year -> year.length == 4 },
            )
        },
        seasons = seasons
            // Season 0 holds specials, which torrents rarely name by season.
            .filter { it.seasonNumber > 0 }
            .map {
                TvSeason(
                    number = it.seasonNumber,
                    name = it.name ?: "Season ${it.seasonNumber}",
                    episodeCount = it.episodeCount ?: 0,
                    posterPath = it.posterPath,
                    airDate = it.airDate,
                )
            },
        lastEpisode = lastEpisodeToAir?.toTvEpisode(),
        nextEpisode = nextEpisodeToAir?.toTvEpisode(),
    )
}

/** The US certification, the one torrent sites and most readers know. */
private fun MovieDetailsDto.certification(isTv: Boolean): String? =
    if (isTv) {
        contentRatings?.results?.firstOrNull { it.country == "US" }?.rating
    } else {
        releaseDates?.results?.firstOrNull { it.country == "US" }
            ?.releaseDates?.firstNotNullOfOrNull { it.certification?.takeIf(String::isNotBlank) }
    }?.takeIf { it.isNotBlank() }

fun TvEpisodeDto.toTvEpisode(): TvEpisode? {
    val season = seasonNumber?.takeIf { it > 0 } ?: return null
    val number = episodeNumber?.takeIf { it > 0 } ?: return null
    return TvEpisode(
        season = season,
        episode = number,
        name = name?.takeIf { it.isNotBlank() },
        airDate = airDate?.takeIf { it.isNotBlank() },
        overview = overview?.takeIf { it.isNotBlank() },
        runtime = runtime?.takeIf { it > 0 },
        stillPath = stillPath?.takeIf { it.isNotBlank() },
    )
}


fun SeasonDetailsDto.toSeasonDetails(number: Int) = SeasonDetails(
    number = seasonNumber ?: number,
    name = name ?: "Season $number",
    overview = overview?.takeIf { it.isNotBlank() },
    posterPath = posterPath?.takeIf { it.isNotBlank() },
    airDate = airDate?.takeIf { it.isNotBlank() },
    episodes = episodes.mapNotNull { it.toTvEpisode() },
)

/**
 * The earliest US cinema, digital and disc dates. A limited release counts as
 * cinemas only when there's no wide one.
 */
private fun MovieDetailsDto.usReleases(): List<Release> =
    releaseDates?.results?.firstOrNull { it.country == "US" }?.releaseDates.orEmpty().toReleases()

/** The earliest theatrical (limited if none), digital and physical date of one country's [ReleaseDateDto]s. */
fun List<ReleaseDateDto>.toReleases(): List<Release> {
    fun earliest(vararg types: Int) = this
        .filter { it.type?.let { type -> type in types } == true }
        .mapNotNull { it.releaseDate?.take(10)?.takeIf { d -> d.length == 10 } }
        .minOrNull()
    return listOfNotNull(
        (earliest(3) ?: earliest(2))?.let { Release(Release.Kind.Theatrical, it) },
        earliest(4)?.let { Release(Release.Kind.Digital, it) },
        earliest(5)?.let { Release(Release.Kind.Physical, it) },
    )
}
