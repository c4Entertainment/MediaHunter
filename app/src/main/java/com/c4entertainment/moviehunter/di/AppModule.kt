package com.c4entertainment.moviehunter.di

import android.content.Context
import androidx.room.Room
import com.c4entertainment.moviehunter.MovieApp
import com.c4entertainment.moviehunter.data.cache.MovieDatabase
import com.c4entertainment.moviehunter.data.network.api.AniListAPI
import com.c4entertainment.moviehunter.data.network.api.KitsuAPI
import com.c4entertainment.moviehunter.data.network.api.DeezerAPI
import com.c4entertainment.moviehunter.data.network.api.OpenLibraryAPI
import com.c4entertainment.moviehunter.data.network.api.RawgAPI
import com.c4entertainment.moviehunter.data.network.api.TMDBMovieAPI
import com.c4entertainment.moviehunter.data.repository.AuthRepositoryImpl
import com.c4entertainment.moviehunter.domain.repository.AuthRepository
import com.c4entertainment.moviehunter.util.ConnectivityObserver
import com.c4entertainment.moviehunter.util.NetworkConnectivityObserver
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideApplication(@ApplicationContext app: Context): MovieApp {
        return app as MovieApp
    }

    @Provides
    @Singleton
    fun provideMovieDatabase(@ApplicationContext context: Context): MovieDatabase {
        return Room.databaseBuilder(
            context,
            MovieDatabase::class.java,
            "movie.db"
        ).build()
    }

    @Provides
    @Singleton
        fun provideMovieApi2(): TMDBMovieAPI {
        return Retrofit.Builder()
            .baseUrl(TMDBMovieAPI.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(TMDBMovieAPI::class.java)
    }


    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideDeezerApi(): DeezerAPI = retrofit(DeezerAPI.BASE_URL).create(DeezerAPI::class.java)

    @Provides
    @Singleton
    fun provideOpenLibraryApi(): OpenLibraryAPI =
        retrofit(OpenLibraryAPI.BASE_URL).create(OpenLibraryAPI::class.java)

    @Provides
    @Singleton
    fun provideRawgApi(): RawgAPI = retrofit(RawgAPI.BASE_URL).create(RawgAPI::class.java)

    @Provides
    @Singleton
    fun provideAniListApi(): AniListAPI = retrofit(AniListAPI.BASE_URL).create(AniListAPI::class.java)

    @Provides
    @Singleton
    fun provideKitsuApi(): KitsuAPI = retrofit(KitsuAPI.BASE_URL).create(KitsuAPI::class.java)

    @Provides
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository = AuthRepositoryImpl(
        auth = Firebase.auth,
        context = context,
    )

    @Provides
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }

}