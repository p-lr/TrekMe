package com.peterlaurence.trekme.features.about.presentation.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.privacyPolicyUrl
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
fun AboutStateful(
    onUserManual: () -> Unit,
    onAppRating: () -> Unit,
    onSendMail: () -> Unit
) {
    val scrollState = rememberScrollState()
    AboutScreen(scrollState, onUserManual, onAppRating, onSendMail)
}

@Composable
fun AboutScreen(
    scrollState: ScrollState,
    onUserManualClick: () -> Unit,
    onAppRating: () -> Unit,
    onSendMail: () -> Unit
) {
    Surface {
        Column(
            Modifier
                .verticalScroll(scrollState)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            UserManualSection(onUserManualClick)
            Spacer(Modifier.height(16.dp))
            AppRatingSection(onAppRating)
            Spacer(Modifier.height(16.dp))
            UserFeedback(onSendMail)
            Spacer(Modifier.height(16.dp))
            PrivacyPolicy()
        }
    }
}

@Composable
private fun ColumnScope.UserManualSection(
    onUserManualClick: () -> Unit
) {
    Text(
        stringResource(
            id = R.string.user_manual_title
        ),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Text(
        stringResource(id = R.string.user_manual_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
    )
    Button(
        modifier = Modifier
            .padding(top = 8.dp)
            .align(Alignment.CenterHorizontally),
        onClick = onUserManualClick,
    ) {
        Text(stringResource(id = R.string.user_manual_btn))
    }
}

@Composable
private fun ColumnScope.AppRatingSection(
    onAppRating: () -> Unit
) {
    Text(
        stringResource(
            id = R.string.rating_title
        ),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Text(
        stringResource(id = R.string.rating_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
    )
    Button(
        modifier = Modifier
            .padding(top = 8.dp)
            .align(Alignment.CenterHorizontally),
        onClick = onAppRating,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
        )
    ) {
        Text(stringResource(id = R.string.rate_the_app))
    }
}

@Composable
private fun ColumnScope.UserFeedback(
    onSendMail: () -> Unit
) {
    Text(
        stringResource(id = R.string.user_feedback),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Text(
        stringResource(id = R.string.feedback_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
    )
    SmallFloatingActionButton(
        onClick = onSendMail,
        shape = CircleShape,
        modifier = Modifier
            .padding(top = 8.dp)
            .align(Alignment.CenterHorizontally),
        containerColor = MaterialTheme.colorScheme.secondary,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
    ) {
        Icon(
            painterResource(id = R.drawable.ic_baseline_mail_outline_24),
            contentDescription = stringResource(id = R.string.mail_button),
            tint = MaterialTheme.colorScheme.onSecondary
        )
    }
}

@Composable
private fun PrivacyPolicy() {
    Text(
        stringResource(id = R.string.privacy_policy_title),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.secondary
    )
    Text(
        stringResource(id = R.string.privacy_policy_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
    )

    val annotatedLinkString = buildAnnotatedString {
        val str = stringResource(id = R.string.privacy_policy_link)
        val placeHolderStr = stringResource(id = R.string.privacy_policy)
        val startIndex = str.indexOf('%')
        val endIndex = startIndex + placeHolderStr.length
        append(str.format(placeHolderStr))
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            ), start = 0, end = endIndex + 1
        )
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.tertiary,
            ), start = startIndex, end = endIndex
        )

        addStringAnnotation(
            tag = "URL",
            annotation = privacyPolicyUrl,
            start = startIndex,
            end = endIndex
        )
    }

    val uriHandler = LocalUriHandler.current

    ClickableText(
        text = annotatedLinkString,
        onClick = {
            annotatedLinkString
                .getStringAnnotations("URL", it, it)
                .firstOrNull()?.let { stringAnnotation ->
                    uriHandler.openUri(stringAnnotation.item)
                }
        }
    )
}


@Preview(locale = "fr")
@Preview(locale = "fr", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun AboutPreview() {
    TrekMeTheme {
        Column(Modifier.size(400.dp, 700.dp)) {
            AboutScreen(
                rememberScrollState(),
                onUserManualClick = {},
                onAppRating = {},
                onSendMail = {}
            )
        }
    }
}