package com.peterlaurence.trekme.main.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.BuildConfig
import com.peterlaurence.trekme.R

@Composable
fun DrawerHeader() {
    val versionName = remember {
        runCatching {
            val version = "v." + BuildConfig.VERSION_NAME
            version
        }.getOrElse { "" }
    }

    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
        Text(text = stringResource(id = R.string.app_name))
        Text(text = versionName, fontSize = 14.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(6.dp))
        HorizontalDivider()
    }
}