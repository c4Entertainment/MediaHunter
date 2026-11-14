package com.c4entertainment.mediahunter.di

import com.c4entertainment.mediahunter.data.repository.CatalogRepositoryImpl
import com.c4entertainment.mediahunter.domain.repository.CatalogRepository
import com.c4entertainment.mediahunter.data.repository.MovieRepositoryImpl
import com.c4entertainment.mediahunter.data.repository.UserSettingsRepositoryImpl
import com.c4entertainment.mediahunter.domain.repository.MovieRepository
import com.c4entertainment.mediahunter.domain.repository.UserSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class  MovieRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(movieRepositoryImpl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(catalogRepositoryImpl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    abstract fun bindsUserDataRepository(
        userDataRepository: UserSettingsRepositoryImpl,
    ): UserSettingsRepository

}