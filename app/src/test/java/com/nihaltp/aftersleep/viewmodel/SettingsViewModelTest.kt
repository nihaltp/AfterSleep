package com.nihaltp.aftersleep.viewmodel

import com.nihaltp.aftersleep.data.AppContainer
import com.nihaltp.aftersleep.data.SettingsRepository
import com.nihaltp.aftersleep.data.model.UserSettings
import com.nihaltp.aftersleep.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `factory creates SettingsViewModel and exposes settings flow`() =
        runTest {
            val settingsRepository = mockk<SettingsRepository>()
            val container = mockk<AppContainer>()
            val settingsFlow = MutableStateFlow(UserSettings(defaultDelayMillis = 120_000L))

            every { container.settingsRepository } returns settingsRepository
            every { settingsRepository.settingsFlow } returns settingsFlow

            val factory = SettingsViewModel.factory(container)
            val viewModel = factory.create(SettingsViewModel::class.java)
            advanceUntilIdle()
            org.junit.Assert.assertNotNull(viewModel)
        }

    @Test
    fun `updateMonochromeMode delegates to repository`() =
        runTest {
            val settingsRepository = mockk<SettingsRepository>()
            val container = mockk<AppContainer>()
            val settingsFlow = MutableStateFlow(UserSettings())

            every { container.settingsRepository } returns settingsRepository
            every { settingsRepository.settingsFlow } returns settingsFlow
            coEvery { settingsRepository.setMonochromeMode(true) } returns Unit

            val viewModel = SettingsViewModel(container)
            viewModel.updateMonochromeMode(true)
            advanceUntilIdle()

            coVerify(exactly = 1) { settingsRepository.setMonochromeMode(true) }
        }
}
