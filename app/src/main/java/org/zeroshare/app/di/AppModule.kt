package org.zeroshare.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.zeroshare.app.data.appextractor.AppPackageExtractor
import org.zeroshare.app.data.connection.ConnectionCoordinator
import org.zeroshare.app.data.connection.LocalHotspotManager
import org.zeroshare.app.data.connection.NsdDiscoveryManager
import org.zeroshare.app.data.connection.WifiDirectManager
import org.zeroshare.app.data.engine.SocketReceiverEngine
import org.zeroshare.app.data.engine.SocketSenderEngine
import org.zeroshare.app.data.storage.MediaPickerRepository
import org.zeroshare.app.data.storage.RawContentReader
import org.zeroshare.app.data.storage.ScopedStorageManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRawContentReader(@ApplicationContext context: Context): RawContentReader {
        return RawContentReader(context)
    }

    @Provides
    @Singleton
    fun provideScopedStorageManager(@ApplicationContext context: Context): ScopedStorageManager {
        return ScopedStorageManager(context)
    }

    @Provides
    @Singleton
    fun provideMediaPickerRepository(@ApplicationContext context: Context): MediaPickerRepository {
        return MediaPickerRepository(context)
    }

    @Provides
    @Singleton
    fun provideAppPackageExtractor(@ApplicationContext context: Context): AppPackageExtractor {
        return AppPackageExtractor(context)
    }

    @Provides
    @Singleton
    fun provideWifiDirectManager(@ApplicationContext context: Context): WifiDirectManager {
        return WifiDirectManager(context)
    }

    @Provides
    @Singleton
    fun provideLocalHotspotManager(@ApplicationContext context: Context): LocalHotspotManager {
        return LocalHotspotManager(context)
    }

    @Provides
    @Singleton
    fun provideNsdDiscoveryManager(@ApplicationContext context: Context): NsdDiscoveryManager {
        return NsdDiscoveryManager(context)
    }

    @Provides
    @Singleton
    fun provideConnectionCoordinator(
        @ApplicationContext context: Context,
        wifiDirectManager: WifiDirectManager,
        localHotspotManager: LocalHotspotManager,
        nsdDiscoveryManager: NsdDiscoveryManager
    ): ConnectionCoordinator {
        return ConnectionCoordinator(context, wifiDirectManager, localHotspotManager, nsdDiscoveryManager)
    }

    @Provides
    @Singleton
    fun provideSocketSenderEngine(rawContentReader: RawContentReader): SocketSenderEngine {
        return SocketSenderEngine(rawContentReader)
    }

    @Provides
    @Singleton
    fun provideSocketReceiverEngine(scopedStorageManager: ScopedStorageManager): SocketReceiverEngine {
        return SocketReceiverEngine(scopedStorageManager)
    }
}
