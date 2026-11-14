package io.github.lyheangtaing.moya.demo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.lyheangtaing.moya.demo.R
import io.github.lyheangtaing.moya.demo.data.GitHubProfileService
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient()

    @Provides
    @Singleton
    fun provideGitHubProfileService(
        @ApplicationContext context: Context,
        client: OkHttpClient,
    ): GitHubProfileService = GitHubProfileService(
        encryptedFixture = {
            context.resources.openRawResource(R.raw.encrypted_profile).use { it.readBytes() }
        },
        callFactory = client,
    )
}
