@file:Suppress("FunctionNaming")

package com.danielealbano.androidremotecontrolmcp.ui.screens.settings

import android.app.LocaleManager
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.danielealbano.androidremotecontrolmcp.R
import com.danielealbano.androidremotecontrolmcp.ui.components.TopBarBackButton
import com.danielealbano.androidremotecontrolmcp.ui.components.TopBarTitle

private data class LanguageOption(
    val languageTag: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int? = null,
)

private val LANGUAGE_OPTIONS =
    listOf(
        LanguageOption(
            languageTag = "",
            titleRes = R.string.language_system_default,
            subtitleRes = R.string.language_system_default_subtitle,
        ),
        LanguageOption(languageTag = "en", titleRes = R.string.language_english),
        LanguageOption(languageTag = "zh-CN", titleRes = R.string.language_simplified_chinese),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val localeManager = remember(context) { context.getSystemService(LocaleManager::class.java) }
    var selectedLanguageTag by remember(localeManager) {
        mutableStateOf(localeManager.applicationLocales.toLanguageTags())
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { TopBarTitle(stringResource(R.string.settings_language_title)) },
            navigationIcon = { TopBarBackButton(onClick = onBack) },
            windowInsets = WindowInsets(0),
        )
        LANGUAGE_OPTIONS.forEach { option ->
            val selected = selectedLanguageTag == option.languageTag
            ListItem(
                headlineContent = { Text(stringResource(option.titleRes)) },
                supportingContent =
                    option.subtitleRes?.let { subtitleRes ->
                        { Text(stringResource(subtitleRes)) }
                    },
                leadingContent = {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                    )
                },
                modifier =
                    Modifier.clickable(
                        role = Role.RadioButton,
                        onClick = {
                            selectedLanguageTag = option.languageTag
                            localeManager.applicationLocales =
                                LocaleList.forLanguageTags(option.languageTag)
                        },
                    ),
            )
        }
    }
}
