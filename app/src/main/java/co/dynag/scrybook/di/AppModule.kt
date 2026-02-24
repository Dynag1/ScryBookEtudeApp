package co.dynag.scrybook.di

import android.content.Context
import co.dynag.scrybook.data.repository.ScryBookRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideScryBookRepository(
        @ApplicationContext context: Context
    ): ScryBookRepository = ScryBookRepository(context)
}
