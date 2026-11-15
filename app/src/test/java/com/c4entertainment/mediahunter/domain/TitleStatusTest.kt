package com.c4entertainment.mediahunter.domain

import com.c4entertainment.mediahunter.domain.model.TitleStatus
import com.c4entertainment.mediahunter.domain.model.TitleStatus.Kind
import com.c4entertainment.mediahunter.domain.model.movie_details.Release
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TitleStatusTest {
    private val today = LocalDate.of(2026, 11, 15)
    private fun theatrical(d: String) = Release(Release.Kind.Theatrical, d)
    private fun digital(d: String) = Release(Release.Kind.Digital, d)
    private fun disc(d: String) = Release(Release.Kind.Physical, d)

    @Test fun announcedWithoutDates() =
        assertEquals(TitleStatus(Kind.Announced), TitleStatus.forMovie(emptyList(), today))

    @Test fun comingUsesTheatricalFirst() =
        assertEquals(
            TitleStatus(Kind.Coming, LocalDate.of(2026, 12, 18)),
            TitleStatus.forMovie(listOf(theatrical("2026-12-18"), digital("2027-02-01")), today),
        )

    @Test fun comingFallsBackToDigital() =
        assertEquals(
            TitleStatus(Kind.Coming, LocalDate.of(2026, 12, 1)),
            TitleStatus.forMovie(listOf(digital("2026-12-01")), today),
        )

    @Test fun inCinemasWithoutDigitalDate() =
        assertEquals(TitleStatus(Kind.InCinemas), TitleStatus.forMovie(listOf(theatrical("2026-11-01")), today))

    @Test fun futureDigitalDateWinsOverCinemas() =
        assertEquals(
            TitleStatus(Kind.DigitalSoon, LocalDate.of(2026, 11, 24)),
            TitleStatus.forMovie(listOf(theatrical("2026-10-23"), digital("2026-11-24")), today),
        )

    @Test fun digitalOnceReleased() =
        assertEquals(
            TitleStatus(Kind.Digital),
            TitleStatus.forMovie(listOf(theatrical("2026-09-01"), digital("2026-11-15"), disc("2026-12-20")), today),
        )

    @Test fun onDiscOnceReleased() =
        assertEquals(
            TitleStatus(Kind.OnDisc),
            TitleStatus.forMovie(listOf(theatrical("2025-01-01"), digital("2025-03-01"), disc("2025-04-01")), today),
        )

    @Test fun returningWithNextEpisode() =
        assertEquals(
            TitleStatus(Kind.NewEpisode, LocalDate.of(2026, 11, 20)),
            TitleStatus.forTv("Returning Series", LocalDate.of(2026, 11, 20)),
        )

    @Test fun returningWithoutNextEpisode() =
        assertEquals(TitleStatus(Kind.Returning), TitleStatus.forTv("Returning Series", null))

    @Test fun endedCancelledAndUpcoming() {
        assertEquals(TitleStatus(Kind.Ended), TitleStatus.forTv("Ended", null))
        assertEquals(TitleStatus(Kind.Cancelled), TitleStatus.forTv("Canceled", null))
        assertEquals(TitleStatus(Kind.Upcoming), TitleStatus.forTv("In Production", null))
        assertEquals(TitleStatus(Kind.Upcoming), TitleStatus.forTv("Planned", null))
    }

    @Test fun unknownTvStatusShowsNothing() = assertNull(TitleStatus.forTv("Something else", null))
}
