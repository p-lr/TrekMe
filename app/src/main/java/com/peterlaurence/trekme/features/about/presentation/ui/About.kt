package com.peterlaurence.trekme.features.about.presentation.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.peterlaurence.trekme.data.backendApi.privacyPolicyUrl
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentColor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor

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
    Column(
        Modifier
            .background(MaterialTheme.colors.surface)
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
        color = MaterialTheme.colors.primary
    )
    Text(
        stringResource(id = R.string.user_manual_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
        color = textColor()
    )
    Button(
        modifier = Modifier
            .padding(top = 8.dp)
            .align(Alignment.CenterHorizontally),
        onClick = onUserManualClick,
        shape = RoundedCornerShape(50)
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
        color = MaterialTheme.colors.primary
    )
    Text(
        stringResource(id = R.string.rating_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
        color = textColor()
    )
    Button(
        modifier = Modifier
            .padding(top = 8.dp)
            .align(Alignment.CenterHorizontally),
        onClick = onAppRating,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = accentColor(),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(50)
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
        color = MaterialTheme.colors.primary
    )
    Text(
        stringResource(id = R.string.feedback_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
        color = textColor()
    )
    FloatingActionButton(
        onClick = onSendMail,
        modifier = Modifier
            .padding(top = 8.dp)
            .size(40.dp)
            .align(Alignment.CenterHorizontally),
        backgroundColor = accentColor(),
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
    ) {
        Image(
            painterResource(id = R.drawable.ic_baseline_mail_outline_24),
            contentDescription = stringResource(id = R.string.mail_button)
        )
    }
}

@Composable
private fun PrivacyPolicy() {
    Text(
        stringResource(id = R.string.privacy_policy_title),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = MaterialTheme.colors.primary
    )
    Text(
        stringResource(id = R.string.privacy_policy_desc),
        modifier = Modifier.padding(top = 8.dp),
        fontWeight = FontWeight.Light,
        color = textColor()
    )

    val annotatedLinkString = buildAnnotatedString {
        val str = stringResource(id = R.string.privacy_policy_link)
        val placeHolderStr = stringResource(id = R.string.privacy_policy)
        val startIndex = str.indexOf('%')
        val endIndex = startIndex + placeHolderStr.length
        append(str.format(placeHolderStr))
        addStyle(
            style = SpanStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                color = textColor()
            ), start = 0, end = endIndex + 1
        )
        addStyle(
            style = SpanStyle(
                color = accentColor(),
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