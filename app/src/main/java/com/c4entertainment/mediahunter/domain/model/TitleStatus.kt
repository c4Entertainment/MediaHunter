package com.c4entertainment.mediahunter.domain.model

import com.c4entertainment.mediahunter.domain.model.movie_details.Release
import java.time.LocalDate

/** Where a title stands today, shown as a pill on its card. [date] is set for the kinds that show one. */
data class TitleStatus(val kind: Kind, val date: LocalDate? = null) {
    enum class Tone { Available, Upcoming, Neutral }

    enum class Kind(val tone: Tone) {
        Coming(Tone.Upcoming),
        Announced(Tone.Neutral),
        InCinemas(Tone.Upcoming),
        DigitalSoon(Tone.Upcoming),
        Digital(Tone.Available),
        OnDisc(Tone.Available),
        NewEpisode(Tone.Upcoming),
        Returning(Tone.Upcoming),
        Ended(Tone.Neutral),
        Cancelled(Tone.Neutral),
        Upcoming(Tone.Upcoming),
    }

    companion object {
        /** Status of a movie from its US [releases] (earliest cinema, digital and disc dates). */
        fun forMovie(releases: List<Release>, today: LocalDate): TitleStatus {
            fun date(kind: Release.Kind) = releases.firstOrNull { it.kind == kind }
                ?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val cinema = date(Release.Kind.Theatrical)
            val digital = date(Release.Kind.Digital)
            val disc = date(Release.Kind.Physical)
            fun past(d: LocalDate?) = d != null && !d.isAfter(today)
            return when {
                past(disc) -> TitleStatus(Kind.OnDisc)
                past(digital) -> TitleStatus(Kind.Digital)
                past(cinema) ->
                    if (digital != null) TitleStatus(Kind.DigitalSoon, digital) else TitleStatus(Kind.InCinemas)
                else -> (cinema ?: digital ?: disc)?.let { TitleStatus(Kind.Coming, it) }
                    ?: TitleStatus(Kind.Announced)
            }
        }

        /** Status of a show from TMDB's [status] and its [nextEpisode] air date; null for statuses we don't label. */
        fun forTv(status: String?, nextEpisode: LocalDate?): TitleStatus? = when (status) {
            "Returning Series" ->
                if (nextEpisode != null) TitleStatus(Kind.NewEpisode, nextEpisode) else TitleStatus(Kind.Returning)
            "Ended" -> TitleStatus(Kind.Ended)
            "Canceled" -> TitleStatus(Kind.Cancelled)
            "In Production", "Planned", "Pilot" -> TitleStatus(Kind.Upcoming)
            else -> null
        }
    }
}
